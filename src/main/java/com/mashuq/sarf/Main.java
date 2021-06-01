package com.mashuq.sarf;

import com.mashuq.sarf.gen.tables.Tasrif;
import com.mashuq.sarf.gen.tables.Verb;
import com.mashuq.sarf.gen.tables.records.TasrifRecord;
import com.mashuq.sarf.gen.tables.records.VerbRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringSubstitutor;
import org.beryx.textio.TextIO;
import org.beryx.textio.TextIoFactory;
import org.beryx.textio.TextTerminal;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

public class Main {

    private static final String attr = "data-stressed";

    private static final String userName = "root";
    private static final String password = "root";
    private static final String url = "jdbc:mysql://127.0.0.1:3306/sarf";

    private static final TextIO textIO = TextIoFactory.getTextIO();
    private static final TextTerminal terminal = textIO.getTextTerminal();

    public static final String subjunctive = "المضارع المنصوب";
    public static final String passive_past = "الماضي المجهول";
    public static final String imperative = "الأمر";
    public static final String past = "الماضي المعلوم";
    public static final String passive_jussive = "المضارع المجهول المجزوم";
    public static final String passive_present = "المضارع المجهول";
    public static final String present = "المضارع المعلوم";
    public static final String jussive = "المضارع المجزوم";
    public static final String passive_subjunctive = "المضارع المجهول المنصوب";

    public static final String hua = "هو";
    public static final String huma1 = "هما";
    public static final String hum = "هم";
    public static final String hiya = "هي";
    public static final String huma2 = "هما مؤ";
    public static final String hunna = "هن";
    public static final String anta = "أنت";
    public static final String antuma1 = "أنتما";
    public static final String antum = "أنتم";
    public static final String anti = "أنتِ";
    public static final String antuma2 = "أنتما مؤ";
    public static final String antunna = "أنتن";
    public static final String ana = "أنا";
    public static final String nahnu = "نحن";

    public static void main(String[] args) {

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(url, userName, password);
            DSLContext dslContext = DSL.using(conn, SQLDialect.MYSQL);
            while (true) {
                int option = textIO.newIntInputReader().withDefaultValue(0).read("0.Exit" +
                        "\n1.Insert Verbs" +
                        "\n2.Process Verbs" +
                        "\n3.Process Remaining Verbs" +
                        "\n4.Make Tasrif of a Verb" +
                        "\n\nSo what should it be?");
                switch (option) {
                    case 1:
                        insertVerbs(dslContext);
                        break;
                    case 2:
                        processVerbs(dslContext);
                        break;
                    case 3:
                        processRemainingVerbs(dslContext);
                        break;
                    case 4:
                        makeTasrif(dslContext);
                        break;
                    case 0:
                        System.exit(0);
                        return;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }

    }

    private static void makeTasrif(DSLContext dslContext) throws IOException {
        String verb = textIO.newStringInputReader().withDefaultValue("نَصَرَ").read("Please provide the verb for making tasrif");
        Result<Record> tasrifs = dslContext.select().from(Tasrif.TASRIF).where(Tasrif.TASRIF.VERB.eq(verb)).limit(1).fetch();
        if (tasrifs.size() == 0) {
            terminal.println("Sorry cannot make tasrif as record does not exist");
        }
        TasrifRecord tasrifRecord = (TasrifRecord) tasrifs.get(0);
        Field<?>[] fields = tasrifRecord.fields();
        Map<String, String> placeholders = new HashMap<>();
        for(Field<?> field : fields){
            placeholders.put(field.getName(), String.valueOf(field.getValue(tasrifRecord)));
        }
        String template = FileUtils.readFileToString(new File("template/tasrif.md"), Charset.defaultCharset());
        StringSubstitutor stringSubstitutor = new StringSubstitutor(placeholders);
        String tasrif = stringSubstitutor.replace(template);
        FileUtils.writeStringToFile(new File(String.format("tasrif/%s.md", verb)), tasrif, Charset.defaultCharset());
    }

    private static void processRemainingVerbs(DSLContext dslContext) {
        Random random = new Random();
        AtomicReference<Integer> count = new AtomicReference<>(0);
        terminal.println("Processing verbs");
        while (true) {
            try {
                Result<Record> verbs = dslContext.select().from(Verb.VERB).where(Verb.VERB.PROCESSED.eq(Byte.valueOf("0"))).orderBy(DSL.rand()).limit(1).fetch();
                if (verbs.size() == 0) {
                    break;
                }
                VerbRecord verbRecord = (VerbRecord) verbs.get(0);
                Document tasrif = getTasrif2(verbRecord.getVerb());
                TasrifRecord tasrifRecord = getSarf2(dslContext, tasrif, verbRecord.getVerb());
                dslContext.transaction(context -> {
                    tasrifRecord.insert();
                    verbRecord.setProcessed(Byte.valueOf("1"));
                    verbRecord.update();
                    count.getAndSet(count.get() + 1);
                });
                terminal.println(String.format("%d processed verb : %s", count.get(), verbRecord.getVerb()));
                try {
                    Thread.sleep(random.nextInt(1000));
                } catch (InterruptedException e) {
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private static String getAttr2(Document doc, String id) {
        return doc.select(String.format("#%s", id)).attr(attr);
    }

    private static TasrifRecord getSarf2(DSLContext dslContext, Document doc, String verb) {
        TasrifRecord tasrifRecord = dslContext.newRecord(Tasrif.TASRIF);

        tasrifRecord.setVerb(verb);

        tasrifRecord.setActivePastThirdpersonMasculineSingular(getAttr2(doc, HtmlIds.past3m.name()));
        tasrifRecord.setActivePastThirdpersonMasculineDual(getAttr2(doc, HtmlIds.pastD3m.name()));
        tasrifRecord.setActivePastThirdpersonMasculinePlural(getAttr2(doc, HtmlIds.past6m.name()));
        tasrifRecord.setActivePastThirdpersonFeminineSingular(getAttr2(doc, HtmlIds.past3f.name()));
        tasrifRecord.setActivePastThirdpersonFeminineDual(getAttr2(doc, HtmlIds.pastD3f.name()));
        tasrifRecord.setActivePastThirdpersonFemininePlural(getAttr2(doc, HtmlIds.past6f.name()));
        tasrifRecord.setActivePastSecondpersonMasculineSingular(getAttr2(doc, HtmlIds.past2m.name()));
        tasrifRecord.setActivePastSecondpersonMasculineDual(getAttr2(doc, HtmlIds.pastD2.name()));
        tasrifRecord.setActivePastSecondpersonMasculinePlural(getAttr2(doc, HtmlIds.past5m.name()));
        tasrifRecord.setActivePastSecondpersonFeminineSingular(getAttr2(doc, HtmlIds.past2f.name()));
        tasrifRecord.setActivePastSecondpersonFeminineDual(getAttr2(doc, HtmlIds.pastD2.name()));
        tasrifRecord.setActivePastSecondpersonFemininePlural(getAttr2(doc, HtmlIds.past5f.name()));
        tasrifRecord.setActivePastFirstpersonMasculineFeminineSingular(getAttr2(doc, HtmlIds.past1.name()));
        tasrifRecord.setActivePastFirstpersonMasculineFemininePlural(getAttr2(doc, HtmlIds.past4.name()));

        tasrifRecord.setActivePresentThirdpersonMasculineSingular(getAttr2(doc, HtmlIds.present3m.name()));
        tasrifRecord.setActivePresentThirdpersonMasculineDual(getAttr2(doc, HtmlIds.presentD3m.name()));
        tasrifRecord.setActivePresentThirdpersonMasculinePlural(getAttr2(doc, HtmlIds.present6m.name()));
        tasrifRecord.setActivePresentThirdpersonFeminineSingular(getAttr2(doc, HtmlIds.present3f.name()));
        tasrifRecord.setActivePresentThirdpersonFeminineDual(getAttr2(doc, HtmlIds.presentD3f.name()));
        tasrifRecord.setActivePresentThirdpersonFemininePlural(getAttr2(doc, HtmlIds.present6f.name()));
        tasrifRecord.setActivePresentSecondpersonMasculineSingular(getAttr2(doc, HtmlIds.present2m.name()));
        tasrifRecord.setActivePresentSecondpersonMasculineDual(getAttr2(doc, HtmlIds.presentD2.name()));
        tasrifRecord.setActivePresentSecondpersonMasculinePlural(getAttr2(doc, HtmlIds.present5m.name()));
        tasrifRecord.setActivePresentSecondpersonFeminineSingular(getAttr2(doc, HtmlIds.present2f.name()));
        tasrifRecord.setActivePresentSecondpersonFeminineDual(getAttr2(doc, HtmlIds.presentD2.name()));
        tasrifRecord.setActivePresentSecondpersonFemininePlural(getAttr2(doc, HtmlIds.present5f.name()));
        tasrifRecord.setActivePresentFirstpersonMasculineFeminineSingular(getAttr2(doc, HtmlIds.present1.name()));
        tasrifRecord.setActivePresentFirstpersonMasculineFemininePlural(getAttr2(doc, HtmlIds.present4.name()));

        tasrifRecord.setActiveSubjunctiveThirdpersonMasculineSingular(getAttr2(doc, HtmlIds.subjunctive3m.name()));
        tasrifRecord.setActiveSubjunctiveThirdpersonMasculineDual(getAttr2(doc, HtmlIds.subjunctiveD3m.name()));
        tasrifRecord.setActiveSubjunctiveThirdpersonMasculinePlural(getAttr2(doc, HtmlIds.subjunctive6m.name()));
        tasrifRecord.setActiveSubjunctiveThirdpersonFeminineSingular(getAttr2(doc, HtmlIds.subjunctive3f.name()));
        tasrifRecord.setActiveSubjunctiveThirdpersonFeminineDual(getAttr2(doc, HtmlIds.subjunctiveD3f.name()));
        tasrifRecord.setActiveSubjunctiveThirdpersonFemininePlural(getAttr2(doc, HtmlIds.subjunctive6f.name()));
        tasrifRecord.setActiveSubjunctiveSecondpersonMasculineSingular(getAttr2(doc, HtmlIds.subjunctive2m.name()));
        tasrifRecord.setActiveSubjunctiveSecondpersonMasculineDual(getAttr2(doc, HtmlIds.subjunctiveD2.name()));
        tasrifRecord.setActiveSubjunctiveSecondpersonMasculinePlural(getAttr2(doc, HtmlIds.subjunctive5m.name()));
        tasrifRecord.setActiveSubjunctiveSecondpersonFeminineSingular(getAttr2(doc, HtmlIds.subjunctive2f.name()));
        tasrifRecord.setActiveSubjunctiveSecondpersonFeminineDual(getAttr2(doc, HtmlIds.subjunctiveD2.name()));
        tasrifRecord.setActiveSubjunctiveSecondpersonFemininePlural(getAttr2(doc, HtmlIds.subjunctive5f.name()));
        tasrifRecord.setActiveSubjunctiveFirstpersonMasculineFeminineSingular(getAttr2(doc, HtmlIds.subjunctive1.name()));
        tasrifRecord.setActiveSubjunctiveFirstpersonMasculineFemininePlural(getAttr2(doc, HtmlIds.subjunctive4.name()));

        tasrifRecord.setActiveJussiveThirdpersonMasculineSingular(getAttr2(doc, HtmlIds.jussive3m.name()));
        tasrifRecord.setActiveJussiveThirdpersonMasculineDual(getAttr2(doc, HtmlIds.jussiveD3m.name()));
        tasrifRecord.setActiveJussiveThirdpersonMasculinePlural(getAttr2(doc, HtmlIds.jussive6m.name()));
        tasrifRecord.setActiveJussiveThirdpersonFeminineSingular(getAttr2(doc, HtmlIds.jussive3f.name()));
        tasrifRecord.setActiveJussiveThirdpersonFeminineDual(getAttr2(doc, HtmlIds.jussiveD3f.name()));
        tasrifRecord.setActiveJussiveThirdpersonFemininePlural(getAttr2(doc, HtmlIds.jussive6f.name()));
        tasrifRecord.setActiveJussiveSecondpersonMasculineSingular(getAttr2(doc, HtmlIds.jussive2m.name()));
        tasrifRecord.setActiveJussiveSecondpersonMasculineDual(getAttr2(doc, HtmlIds.jussiveD2.name()));
        tasrifRecord.setActiveJussiveSecondpersonMasculinePlural(getAttr2(doc, HtmlIds.jussive5m.name()));
        tasrifRecord.setActiveJussiveSecondpersonFeminineSingular(getAttr2(doc, HtmlIds.jussive2f.name()));
        tasrifRecord.setActiveJussiveSecondpersonFeminineDual(getAttr2(doc, HtmlIds.jussiveD2.name()));
        tasrifRecord.setActiveJussiveSecondpersonFemininePlural(getAttr2(doc, HtmlIds.jussive5f.name()));
        tasrifRecord.setActiveJussiveFirstpersonMasculineFeminineSingular(getAttr2(doc, HtmlIds.jussive1.name()));
        tasrifRecord.setActiveJussiveFirstpersonMasculineFemininePlural(getAttr2(doc, HtmlIds.jussive4.name()));

        tasrifRecord.setPassivePastThirdpersonMasculineSingular(getAttr2(doc, HtmlIds.passive_past3m.name()));
        tasrifRecord.setPassivePastThirdpersonMasculineDual(getAttr2(doc, HtmlIds.passive_pastD3m.name()));
        tasrifRecord.setPassivePastThirdpersonMasculinePlural(getAttr2(doc, HtmlIds.passive_past6m.name()));
        tasrifRecord.setPassivePastThirdpersonFeminineSingular(getAttr2(doc, HtmlIds.passive_past3f.name()));
        tasrifRecord.setPassivePastThirdpersonFeminineDual(getAttr2(doc, HtmlIds.passive_pastD3f.name()));
        tasrifRecord.setPassivePastThirdpersonFemininePlural(getAttr2(doc, HtmlIds.passive_past6f.name()));
        tasrifRecord.setPassivePastSecondpersonMasculineSingular(getAttr2(doc, HtmlIds.passive_past2m.name()));
        tasrifRecord.setPassivePastSecondpersonMasculineDual(getAttr2(doc, HtmlIds.passive_pastD2.name()));
        tasrifRecord.setPassivePastSecondpersonMasculinePlural(getAttr2(doc, HtmlIds.passive_past5m.name()));
        tasrifRecord.setPassivePastSecondpersonFeminineSingular(getAttr2(doc, HtmlIds.passive_past2f.name()));
        tasrifRecord.setPassivePastSecondpersonFeminineDual(getAttr2(doc, HtmlIds.passive_pastD2.name()));
        tasrifRecord.setPassivePastSecondpersonFemininePlural(getAttr2(doc, HtmlIds.passive_past5f.name()));
        tasrifRecord.setPassivePastFirstpersonMasculineFeminineSingular(getAttr2(doc, HtmlIds.passive_past1.name()));
        tasrifRecord.setPassivePastFirstpersonMasculineFemininePlural(getAttr2(doc, HtmlIds.passive_past4.name()));

        tasrifRecord.setPassivePresentThirdpersonMasculineSingular(getAttr2(doc, HtmlIds.passive_present3m.name()));
        tasrifRecord.setPassivePresentThirdpersonMasculineDual(getAttr2(doc, HtmlIds.passive_presentD3m.name()));
        tasrifRecord.setPassivePresentThirdpersonMasculinePlural(getAttr2(doc, HtmlIds.passive_present6m.name()));
        tasrifRecord.setPassivePresentThirdpersonFeminineSingular(getAttr2(doc, HtmlIds.passive_present3f.name()));
        tasrifRecord.setPassivePresentThirdpersonFeminineDual(getAttr2(doc, HtmlIds.passive_presentD3f.name()));
        tasrifRecord.setPassivePresentThirdpersonFemininePlural(getAttr2(doc, HtmlIds.passive_present6f.name()));
        tasrifRecord.setPassivePresentSecondpersonMasculineSingular(getAttr2(doc, HtmlIds.passive_present2m.name()));
        tasrifRecord.setPassivePresentSecondpersonMasculineDual(getAttr2(doc, HtmlIds.passive_presentD2.name()));
        tasrifRecord.setPassivePresentSecondpersonMasculinePlural(getAttr2(doc, HtmlIds.passive_present5m.name()));
        tasrifRecord.setPassivePresentSecondpersonFeminineSingular(getAttr2(doc, HtmlIds.passive_present2f.name()));
        tasrifRecord.setPassivePresentSecondpersonFeminineDual(getAttr2(doc, HtmlIds.passive_presentD2.name()));
        tasrifRecord.setPassivePresentSecondpersonFemininePlural(getAttr2(doc, HtmlIds.passive_present5f.name()));
        tasrifRecord.setPassivePresentFirstpersonMasculineFeminineSingular(getAttr2(doc, HtmlIds.passive_present1.name()));
        tasrifRecord.setPassivePresentFirstpersonMasculineFemininePlural(getAttr2(doc, HtmlIds.passive_present4.name()));

        tasrifRecord.setPassiveSubjunctiveThirdpersonMasculineSingular(getAttr2(doc, HtmlIds.passive_subjunctive3m.name()));
        tasrifRecord.setPassiveSubjunctiveThirdpersonMasculineDual(getAttr2(doc, HtmlIds.passive_subjunctiveD3m.name()));
        tasrifRecord.setPassiveSubjunctiveThirdpersonMasculinePlural(getAttr2(doc, HtmlIds.passive_subjunctive6m.name()));
        tasrifRecord.setPassiveSubjunctiveThirdpersonFeminineSingular(getAttr2(doc, HtmlIds.passive_subjunctive3f.name()));
        tasrifRecord.setPassiveSubjunctiveThirdpersonFeminineDual(getAttr2(doc, HtmlIds.passive_subjunctiveD3f.name()));
        tasrifRecord.setPassiveSubjunctiveThirdpersonFemininePlural(getAttr2(doc, HtmlIds.passive_subjunctive6f.name()));
        tasrifRecord.setPassiveSubjunctiveSecondpersonMasculineSingular(getAttr2(doc, HtmlIds.passive_subjunctive2m.name()));
        tasrifRecord.setPassiveSubjunctiveSecondpersonMasculineDual(getAttr2(doc, HtmlIds.passive_subjunctiveD2.name()));
        tasrifRecord.setPassiveSubjunctiveSecondpersonMasculinePlural(getAttr2(doc, HtmlIds.passive_subjunctive5m.name()));
        tasrifRecord.setPassiveSubjunctiveSecondpersonFeminineSingular(getAttr2(doc, HtmlIds.passive_subjunctive2f.name()));
        tasrifRecord.setPassiveSubjunctiveSecondpersonFeminineDual(getAttr2(doc, HtmlIds.passive_subjunctiveD2.name()));
        tasrifRecord.setPassiveSubjunctiveSecondpersonFemininePlural(getAttr2(doc, HtmlIds.passive_subjunctive5f.name()));
        tasrifRecord.setPassiveSubjunctiveFirstpersonMasculineFeminineSingular(getAttr2(doc, HtmlIds.passive_subjunctive1.name()));
        tasrifRecord.setPassiveSubjunctiveFirstpersonMasculineFemininePlural(getAttr2(doc, HtmlIds.passive_subjunctive4.name()));

        tasrifRecord.setPassiveJussiveThirdpersonMasculineSingular(getAttr2(doc, HtmlIds.passive_jussive3m.name()));
        tasrifRecord.setPassiveJussiveThirdpersonMasculineDual(getAttr2(doc, HtmlIds.passive_jussiveD3m.name()));
        tasrifRecord.setPassiveJussiveThirdpersonMasculinePlural(getAttr2(doc, HtmlIds.passive_jussive6m.name()));
        tasrifRecord.setPassiveJussiveThirdpersonFeminineSingular(getAttr2(doc, HtmlIds.passive_jussive3f.name()));
        tasrifRecord.setPassiveJussiveThirdpersonFeminineDual(getAttr2(doc, HtmlIds.passive_jussiveD3f.name()));
        tasrifRecord.setPassiveJussiveThirdpersonFemininePlural(getAttr2(doc, HtmlIds.passive_jussive6f.name()));
        tasrifRecord.setPassiveJussiveSecondpersonMasculineSingular(getAttr2(doc, HtmlIds.passive_jussive2m.name()));
        tasrifRecord.setPassiveJussiveSecondpersonMasculineDual(getAttr2(doc, HtmlIds.passive_jussiveD2.name()));
        tasrifRecord.setPassiveJussiveSecondpersonMasculinePlural(getAttr2(doc, HtmlIds.passive_jussive5m.name()));
        tasrifRecord.setPassiveJussiveSecondpersonFeminineSingular(getAttr2(doc, HtmlIds.passive_jussive2f.name()));
        tasrifRecord.setPassiveJussiveSecondpersonFeminineDual(getAttr2(doc, HtmlIds.passive_jussiveD2.name()));
        tasrifRecord.setPassiveJussiveSecondpersonFemininePlural(getAttr2(doc, HtmlIds.passive_jussive5f.name()));
        tasrifRecord.setPassiveJussiveFirstpersonMasculineFeminineSingular(getAttr2(doc, HtmlIds.passive_jussive1.name()));
        tasrifRecord.setPassiveJussiveFirstpersonMasculineFemininePlural(getAttr2(doc, HtmlIds.passive_jussive4.name()));

        tasrifRecord.setImperativeSecondpersonMasculineSingular(getAttr2(doc, HtmlIds.imperative2m.name()));
        tasrifRecord.setImperativeSecondpersonFeminineSingular(getAttr2(doc, HtmlIds.imperative2f.name()));
        tasrifRecord.setImperativeSecondpersonMasculineFeminineDual(getAttr2(doc, HtmlIds.imperativeD2.name()));
        tasrifRecord.setImperativeSecondpersonMasculinePlural(getAttr2(doc, HtmlIds.imperative5m.name()));
        tasrifRecord.setImperativeSecondpersonFemininePlural(getAttr2(doc, HtmlIds.imperative5f.name()));

        tasrifRecord.setActiveParticiple(getAttr2(doc, HtmlIds.active_participle.name()));
        tasrifRecord.setPassiveParticiple(getAttr2(doc, HtmlIds.passive_participle.name()));
        tasrifRecord.setVerbalNoun(getAttr2(doc, HtmlIds.verbal_noun.name()));

        return tasrifRecord;
    }

    private static Document getTasrif2(String verb) throws IOException {
        return Jsoup.connect(String.format("https://cooljugator.com/ar/%s", verb)).get();
    }

    private static void processVerbs(DSLContext dslContext) {
        Random random = new Random();
        AtomicReference<Integer> count = new AtomicReference<>(0);
        terminal.println("Processing verbs");
        while (true) {
            try {
                Result<Record> verbs = dslContext.select().from(Verb.VERB).where(Verb.VERB.PROCESSED.eq(Byte.valueOf("0"))).orderBy(DSL.rand()).limit(1).fetch();
                if (verbs.size() == 0) {
                    break;
                }
                VerbRecord verbRecord = (VerbRecord) verbs.get(0);
                String tasrif = getTasrif(verbRecord.getVerb());
                TasrifRecord tasrifRecord = getSarf(dslContext, tasrif, verbRecord.getVerb());
                dslContext.transaction(context -> {
                    tasrifRecord.insert();
                    verbRecord.setProcessed(Byte.valueOf("1"));
                    verbRecord.update();
                    count.getAndSet(count.get() + 1);
                });
                terminal.println(String.format("%d processed verb : %s", count.get(), verbRecord.getVerb()));
                try {
                    Thread.sleep(random.nextInt(1000));
                } catch (InterruptedException e) {
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private static TasrifRecord getSarf(DSLContext dslContext, String tasrif, String verb) throws IOException {
        JsonReader jsonReader = Json.createReader(new StringReader(tasrif));
        JsonObject jsonObject = jsonReader.readObject();

        TasrifRecord tasrifRecord = dslContext.newRecord(Tasrif.TASRIF);
        tasrifRecord.setVerb(verb);


        tasrifRecord.setActivePastThirdpersonMasculineSingular(getAttr(jsonObject, past, hua));
        tasrifRecord.setActivePastThirdpersonMasculineDual(getAttr(jsonObject, past, huma1));
        tasrifRecord.setActivePastThirdpersonMasculinePlural(getAttr(jsonObject, past, hum));
        tasrifRecord.setActivePastThirdpersonFeminineSingular(getAttr(jsonObject, past, hiya));
        tasrifRecord.setActivePastThirdpersonFeminineDual(getAttr(jsonObject, past, huma2));
        tasrifRecord.setActivePastThirdpersonFemininePlural(getAttr(jsonObject, past, hunna));
        tasrifRecord.setActivePastSecondpersonMasculineSingular(getAttr(jsonObject, past, anta));
        tasrifRecord.setActivePastSecondpersonMasculineDual(getAttr(jsonObject, past, antuma1));
        tasrifRecord.setActivePastSecondpersonMasculinePlural(getAttr(jsonObject, past, antum));
        tasrifRecord.setActivePastSecondpersonFeminineSingular(getAttr(jsonObject, past, anti));
        tasrifRecord.setActivePastSecondpersonFeminineDual(getAttr(jsonObject, past, antuma2));
        tasrifRecord.setActivePastSecondpersonFemininePlural(getAttr(jsonObject, past, antunna));
        tasrifRecord.setActivePastFirstpersonMasculineFeminineSingular(getAttr(jsonObject, past, ana));
        tasrifRecord.setActivePastFirstpersonMasculineFemininePlural(getAttr(jsonObject, past, nahnu));

        tasrifRecord.setActivePresentThirdpersonMasculineSingular(getAttr(jsonObject, present, hua));
        tasrifRecord.setActivePresentThirdpersonMasculineDual(getAttr(jsonObject, present, huma1));
        tasrifRecord.setActivePresentThirdpersonMasculinePlural(getAttr(jsonObject, present, hum));
        tasrifRecord.setActivePresentThirdpersonFeminineSingular(getAttr(jsonObject, present, hiya));
        tasrifRecord.setActivePresentThirdpersonFeminineDual(getAttr(jsonObject, present, huma2));
        tasrifRecord.setActivePresentThirdpersonFemininePlural(getAttr(jsonObject, present, hunna));
        tasrifRecord.setActivePresentSecondpersonMasculineSingular(getAttr(jsonObject, present, anta));
        tasrifRecord.setActivePresentSecondpersonMasculineDual(getAttr(jsonObject, present, antuma1));
        tasrifRecord.setActivePresentSecondpersonMasculinePlural(getAttr(jsonObject, present, antum));
        tasrifRecord.setActivePresentSecondpersonFeminineSingular(getAttr(jsonObject, present, anti));
        tasrifRecord.setActivePresentSecondpersonFeminineDual(getAttr(jsonObject, present, antuma2));
        tasrifRecord.setActivePresentSecondpersonFemininePlural(getAttr(jsonObject, present, antunna));
        tasrifRecord.setActivePresentFirstpersonMasculineFeminineSingular(getAttr(jsonObject, present, ana));
        tasrifRecord.setActivePresentFirstpersonMasculineFemininePlural(getAttr(jsonObject, present, nahnu));

        tasrifRecord.setActiveSubjunctiveThirdpersonMasculineSingular(getAttr(jsonObject, subjunctive, hua));
        tasrifRecord.setActiveSubjunctiveThirdpersonMasculineDual(getAttr(jsonObject, subjunctive, huma1));
        tasrifRecord.setActiveSubjunctiveThirdpersonMasculinePlural(getAttr(jsonObject, subjunctive, hum));
        tasrifRecord.setActiveSubjunctiveThirdpersonFeminineSingular(getAttr(jsonObject, subjunctive, hiya));
        tasrifRecord.setActiveSubjunctiveThirdpersonFeminineDual(getAttr(jsonObject, subjunctive, huma2));
        tasrifRecord.setActiveSubjunctiveThirdpersonFemininePlural(getAttr(jsonObject, subjunctive, hunna));
        tasrifRecord.setActiveSubjunctiveSecondpersonMasculineSingular(getAttr(jsonObject, subjunctive, anta));
        tasrifRecord.setActiveSubjunctiveSecondpersonMasculineDual(getAttr(jsonObject, subjunctive, antuma1));
        tasrifRecord.setActiveSubjunctiveSecondpersonMasculinePlural(getAttr(jsonObject, subjunctive, antum));
        tasrifRecord.setActiveSubjunctiveSecondpersonFeminineSingular(getAttr(jsonObject, subjunctive, anti));
        tasrifRecord.setActiveSubjunctiveSecondpersonFeminineDual(getAttr(jsonObject, subjunctive, antuma2));
        tasrifRecord.setActiveSubjunctiveSecondpersonFemininePlural(getAttr(jsonObject, subjunctive, antunna));
        tasrifRecord.setActiveSubjunctiveFirstpersonMasculineFeminineSingular(getAttr(jsonObject, subjunctive, ana));
        tasrifRecord.setActiveSubjunctiveFirstpersonMasculineFemininePlural(getAttr(jsonObject, subjunctive, nahnu));

        tasrifRecord.setActiveJussiveThirdpersonMasculineSingular(getAttr(jsonObject, jussive, hua));
        tasrifRecord.setActiveJussiveThirdpersonMasculineDual(getAttr(jsonObject, jussive, huma1));
        tasrifRecord.setActiveJussiveThirdpersonMasculinePlural(getAttr(jsonObject, jussive, hum));
        tasrifRecord.setActiveJussiveThirdpersonFeminineSingular(getAttr(jsonObject, jussive, hiya));
        tasrifRecord.setActiveJussiveThirdpersonFeminineDual(getAttr(jsonObject, jussive, huma2));
        tasrifRecord.setActiveJussiveThirdpersonFemininePlural(getAttr(jsonObject, jussive, hunna));
        tasrifRecord.setActiveJussiveSecondpersonMasculineSingular(getAttr(jsonObject, jussive, anta));
        tasrifRecord.setActiveJussiveSecondpersonMasculineDual(getAttr(jsonObject, jussive, antuma1));
        tasrifRecord.setActiveJussiveSecondpersonMasculinePlural(getAttr(jsonObject, jussive, antum));
        tasrifRecord.setActiveJussiveSecondpersonFeminineSingular(getAttr(jsonObject, jussive, anti));
        tasrifRecord.setActiveJussiveSecondpersonFeminineDual(getAttr(jsonObject, jussive, antuma2));
        tasrifRecord.setActiveJussiveSecondpersonFemininePlural(getAttr(jsonObject, jussive, antunna));
        tasrifRecord.setActiveJussiveFirstpersonMasculineFeminineSingular(getAttr(jsonObject, jussive, ana));
        tasrifRecord.setActiveJussiveFirstpersonMasculineFemininePlural(getAttr(jsonObject, jussive, nahnu));

        tasrifRecord.setPassivePastThirdpersonMasculineSingular(getAttr(jsonObject, passive_past, hua));
        tasrifRecord.setPassivePastThirdpersonMasculineDual(getAttr(jsonObject, passive_past, huma1));
        tasrifRecord.setPassivePastThirdpersonMasculinePlural(getAttr(jsonObject, passive_past, hum));
        tasrifRecord.setPassivePastThirdpersonFeminineSingular(getAttr(jsonObject, passive_past, hiya));
        tasrifRecord.setPassivePastThirdpersonFeminineDual(getAttr(jsonObject, passive_past, huma2));
        tasrifRecord.setPassivePastThirdpersonFemininePlural(getAttr(jsonObject, passive_past, hunna));
        tasrifRecord.setPassivePastSecondpersonMasculineSingular(getAttr(jsonObject, passive_past, anta));
        tasrifRecord.setPassivePastSecondpersonMasculineDual(getAttr(jsonObject, passive_past, antuma1));
        tasrifRecord.setPassivePastSecondpersonMasculinePlural(getAttr(jsonObject, passive_past, antum));
        tasrifRecord.setPassivePastSecondpersonFeminineSingular(getAttr(jsonObject, passive_past, anti));
        tasrifRecord.setPassivePastSecondpersonFeminineDual(getAttr(jsonObject, passive_past, antuma2));
        tasrifRecord.setPassivePastSecondpersonFemininePlural(getAttr(jsonObject, passive_past, antunna));
        tasrifRecord.setPassivePastFirstpersonMasculineFeminineSingular(getAttr(jsonObject, passive_past, ana));
        tasrifRecord.setPassivePastFirstpersonMasculineFemininePlural(getAttr(jsonObject, passive_past, nahnu));

        tasrifRecord.setPassivePresentThirdpersonMasculineSingular(getAttr(jsonObject, passive_present, hua));
        tasrifRecord.setPassivePresentThirdpersonMasculineDual(getAttr(jsonObject, passive_present, huma1));
        tasrifRecord.setPassivePresentThirdpersonMasculinePlural(getAttr(jsonObject, passive_present, hum));
        tasrifRecord.setPassivePresentThirdpersonFeminineSingular(getAttr(jsonObject, passive_present, hiya));
        tasrifRecord.setPassivePresentThirdpersonFeminineDual(getAttr(jsonObject, passive_present, huma2));
        tasrifRecord.setPassivePresentThirdpersonFemininePlural(getAttr(jsonObject, passive_present, hunna));
        tasrifRecord.setPassivePresentSecondpersonMasculineSingular(getAttr(jsonObject, passive_present, anta));
        tasrifRecord.setPassivePresentSecondpersonMasculineDual(getAttr(jsonObject, passive_present, antuma1));
        tasrifRecord.setPassivePresentSecondpersonMasculinePlural(getAttr(jsonObject, passive_present, antum));
        tasrifRecord.setPassivePresentSecondpersonFeminineSingular(getAttr(jsonObject, passive_present, anti));
        tasrifRecord.setPassivePresentSecondpersonFeminineDual(getAttr(jsonObject, passive_present, antuma2));
        tasrifRecord.setPassivePresentSecondpersonFemininePlural(getAttr(jsonObject, passive_present, antunna));
        tasrifRecord.setPassivePresentFirstpersonMasculineFeminineSingular(getAttr(jsonObject, passive_present, ana));
        tasrifRecord.setPassivePresentFirstpersonMasculineFemininePlural(getAttr(jsonObject, passive_present, nahnu));

        tasrifRecord.setPassiveSubjunctiveThirdpersonMasculineSingular(getAttr(jsonObject, passive_subjunctive, hua));
        tasrifRecord.setPassiveSubjunctiveThirdpersonMasculineDual(getAttr(jsonObject, passive_subjunctive, huma1));
        tasrifRecord.setPassiveSubjunctiveThirdpersonMasculinePlural(getAttr(jsonObject, passive_subjunctive, hum));
        tasrifRecord.setPassiveSubjunctiveThirdpersonFeminineSingular(getAttr(jsonObject, passive_subjunctive, hiya));
        tasrifRecord.setPassiveSubjunctiveThirdpersonFeminineDual(getAttr(jsonObject, passive_subjunctive, huma2));
        tasrifRecord.setPassiveSubjunctiveThirdpersonFemininePlural(getAttr(jsonObject, passive_subjunctive, hunna));
        tasrifRecord.setPassiveSubjunctiveSecondpersonMasculineSingular(getAttr(jsonObject, passive_subjunctive, anta));
        tasrifRecord.setPassiveSubjunctiveSecondpersonMasculineDual(getAttr(jsonObject, passive_subjunctive, antuma1));
        tasrifRecord.setPassiveSubjunctiveSecondpersonMasculinePlural(getAttr(jsonObject, passive_subjunctive, antum));
        tasrifRecord.setPassiveSubjunctiveSecondpersonFeminineSingular(getAttr(jsonObject, passive_subjunctive, anti));
        tasrifRecord.setPassiveSubjunctiveSecondpersonFeminineDual(getAttr(jsonObject, passive_subjunctive, antuma2));
        tasrifRecord.setPassiveSubjunctiveSecondpersonFemininePlural(getAttr(jsonObject, passive_subjunctive, antunna));
        tasrifRecord.setPassiveSubjunctiveFirstpersonMasculineFeminineSingular(getAttr(jsonObject, passive_subjunctive, ana));
        tasrifRecord.setPassiveSubjunctiveFirstpersonMasculineFemininePlural(getAttr(jsonObject, passive_subjunctive, nahnu));

        tasrifRecord.setPassiveJussiveThirdpersonMasculineSingular(getAttr(jsonObject, passive_jussive, hua));
        tasrifRecord.setPassiveJussiveThirdpersonMasculineDual(getAttr(jsonObject, passive_jussive, huma1));
        tasrifRecord.setPassiveJussiveThirdpersonMasculinePlural(getAttr(jsonObject, passive_jussive, hum));
        tasrifRecord.setPassiveJussiveThirdpersonFeminineSingular(getAttr(jsonObject, passive_jussive, hiya));
        tasrifRecord.setPassiveJussiveThirdpersonFeminineDual(getAttr(jsonObject, passive_jussive, huma2));
        tasrifRecord.setPassiveJussiveThirdpersonFemininePlural(getAttr(jsonObject, passive_jussive, hunna));
        tasrifRecord.setPassiveJussiveSecondpersonMasculineSingular(getAttr(jsonObject, passive_jussive, anta));
        tasrifRecord.setPassiveJussiveSecondpersonMasculineDual(getAttr(jsonObject, passive_jussive, antuma1));
        tasrifRecord.setPassiveJussiveSecondpersonMasculinePlural(getAttr(jsonObject, passive_jussive, antum));
        tasrifRecord.setPassiveJussiveSecondpersonFeminineSingular(getAttr(jsonObject, passive_jussive, anti));
        tasrifRecord.setPassiveJussiveSecondpersonFeminineDual(getAttr(jsonObject, passive_jussive, antuma2));
        tasrifRecord.setPassiveJussiveSecondpersonFemininePlural(getAttr(jsonObject, passive_jussive, antunna));
        tasrifRecord.setPassiveJussiveFirstpersonMasculineFeminineSingular(getAttr(jsonObject, passive_jussive, ana));
        tasrifRecord.setPassiveJussiveFirstpersonMasculineFemininePlural(getAttr(jsonObject, passive_jussive, nahnu));

        tasrifRecord.setImperativeSecondpersonMasculineSingular(getAttr(jsonObject, imperative, anta));
        tasrifRecord.setImperativeSecondpersonFeminineSingular(getAttr(jsonObject, imperative, anti));
        tasrifRecord.setImperativeSecondpersonMasculineFeminineDual(getAttr(jsonObject, imperative, antuma1));
        tasrifRecord.setImperativeSecondpersonMasculinePlural(getAttr(jsonObject, imperative, antum));
        tasrifRecord.setImperativeSecondpersonFemininePlural(getAttr(jsonObject, imperative, antunna));

        return tasrifRecord;
    }

    private static String getAttr(JsonObject jsonObject, String tense, String sigah) throws IOException {
        try {
            return jsonObject.getJsonObject(tense).getString(sigah);
        }catch(Exception ex){
            String error = String.format("---\n%s\n%s\n%s\n---", jsonObject.toString(), tense, sigah);
            File file = new File("errors.txt");
            FileUtils.writeStringToFile(file, error, Charset.defaultCharset(), true);
        }
        return null;
    }

    private static String getTasrif(String fil) {
        try {
            String verb = URLEncoder.encode(fil);
            String cmd = "curl https://qutrub.arabeyes.org/?verb=" + verb + "&transitive=1&all=1&past=1&future=1&passive=1&future_moode=1&confirmed=1&haraka=%D9%81%D8%AA%D8%AD%D8%A9&display_format=HTML -H User-Agent: Mozilla/5.0 (X11; Fedora; Linux x86_64; rv:79.0) Gecko/20100101 Firefox/79.0 -H Accept: application/json, text/javascript, */* -H Accept-Language: en-US,en;q=0.5 --compressed -H X-Requested-With: XMLHttpRequest -H Connection: keep-alive -H Referer: https://qutrub.arabeyes.org/";
            String text = execCmd(cmd);
            System.out.println(text);
            int index = text.indexOf("<");
            if (index > 0) {
                text = text.substring(0, index);
            }
            return text;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static String execCmd(String cmd) throws java.io.IOException {
        java.util.Scanner s = new java.util.Scanner(Runtime.getRuntime().exec(cmd).getInputStream()).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    private static void insertVerbs(DSLContext dslContext) throws IOException {
        terminal.print("Inserting verbs...");
        List<String> verbs = FileUtils.readLines(new File("data/fel.txt"), "UTF-8");
        int count = 1;
        String currentVerb = null;
        for (String verb : verbs) {
            try {
                currentVerb = verb;
                VerbRecord verbListRecord = dslContext.newRecord(Verb.VERB);
                verbListRecord.setVerb(verb);
                verbListRecord.insert();
                terminal.println(String.format("%d Inserted verb : %s", count, verb));
                count++;
            } catch (Exception ex) {
                terminal.println(String.format("%d Duplicate verb : %s", count, currentVerb));
            }
        }
    }
}

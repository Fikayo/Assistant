package edu.cmu.ark;

public class QuestionAskerOptions
{
    private boolean printVerbose = false;
    private String modelPath = null;
    private boolean preferWH = false;
    private boolean doNonPronounNPC = false;
    private boolean doPronounNPC = true;
    private Integer maxLength = 1000;
    private boolean downweightPronouns = false;
    private boolean avoidFreqWords = false;
    private boolean dropPro = true;
    private boolean justWH = false;
    private boolean debug = false;
    private String properties = null;

    private QuestionAskerOptions() {}

    public boolean isPrintVerbose() {
        return printVerbose;
    }

    public String getModelPath() {
        return modelPath;
    }

    public boolean isPreferWH() {
        return preferWH;
    }

    public boolean isDoNonPronounNPC() {
        return doNonPronounNPC;
    }

    public boolean isDoPronounNPC() {
        return doPronounNPC;
    }

    public Integer getMaxLength() {
        return maxLength;
    }

    public boolean isDownweightPronouns() {
        return downweightPronouns;
    }

    public boolean isAvoidFreqWords() {
        return avoidFreqWords;
    }

    public boolean isDropPro() {
        return dropPro;
    }

    public boolean isJustWH() {
        return justWH;
    }

    public boolean isDebug() {
        return debug;
    }

    public String getProperties() {
        return properties;
    }

    public static QuestionAskerOptions empty() {
        return new QuestionAskerOptions();
    }

    public static QuestionAskerOptions parse(String[] args) {

        QuestionAskerOptions options = QuestionAskerOptions.empty();

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--debug")) {
                options.debug = true;
            } else if (args[i].equals("--verbose")) {
                options.printVerbose = true;
            } else if (args[i].equals("--model")) { //ranking model path
                options.modelPath = args[i + 1];
                i++;
            } else if (args[i].equals("--keep-pro")) {
                options.dropPro = false;
            } else if (args[i].equals("--downweight-pro")) {
                options.dropPro = false;
                options.downweightPronouns = true;
            } else if (args[i].equals("--downweight-frequent-answers")) {
                options.avoidFreqWords = true;
            } else if (args[i].equals("--properties")) {
                GlobalProperties.loadProperties(args[i + 1]);
                options.properties = args[i + 1];
            } else if (args[i].equals("--prefer-wh")) {
                options.preferWH = true;
            } else if (args[i].equals("--just-wh")) {
                options.justWH = true;
            } else if (args[i].equals("--full-npc")) {
                options.doNonPronounNPC = true;
            } else if (args[i].equals("--no-npc")) {
                options.doPronounNPC = false;
            } else if (args[i].equals("--max-length")) {
                options.maxLength = new Integer(args[i + 1]);
                i++;
            }
        }

        return options;
    }
}
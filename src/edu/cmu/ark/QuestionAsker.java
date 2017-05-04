// Question Generation via Overgenerating Transformations and Ranking
// Copyright (c) 2008, 2009 Carnegie Mellon University.  All Rights Reserved.
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// For more information, bug reports, fixes, contact:
//    Michael Heilman
//	  Carnegie Mellon University
//	  mheilman@cmu.edu
//	  http://www.cs.cmu.edu/~mheilman


package edu.cmu.ark;

import java.io.*;
//import java.text.NumberFormat;
import java.util.*;

//import weka.classifiers.functions.LinearRegression;

//import edu.cmu.ark.ranking.WekaLinearRegressionRanker;
import edu.stanford.nlp.trees.Tree;


/**
 * Wrapper class for outputting a (ranked) list of questions given an entire document,
 * not just a sentence.  It wraps the three stages discussed in the technical report and calls each in turn
 * (along with parsing and other preprocessing) to produce questions.
 * <p>
 * This is the typical class to use for running the system via the command line.
 * <p>
 * Example usage:
 * <p>
 * java -server -Xmx800m -cp lib/weka-3-6.jar:lib/stanford-parser-2008-10-26.jar:bin:lib/jwnl.jar:lib/commons-logging.jar:lib/commons-lang-2.4.jar:lib/supersense-tagger.jar:lib/stanford-ner-2008-05-07.jar:lib/arkref.jar \
 * edu/cmu/ark/QuestionAsker \
 * --verbose --simplify --group \
 * --model models/linear-regression-ranker-06-24-2010.ser.gz \
 * --prefer-wh --max-length 30 --downweight-pro
 *
 * @author mheilman@cs.cmu.edu
 */
public class QuestionAsker {


    public QuestionAsker() {
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        QuestionTransducer qt = new QuestionTransducer();
        InitialTransformationStep trans = new InitialTransformationStep();
        QuestionRanker qr = null;


        List<Question> outputQuestionList = new ArrayList<>();
        qt.setAvoidPronounsAndDemonstratives(false);

        //pre-load
        AnalysisUtilities.getInstance();

        String buf;
        Tree parsed;
        QuestionAskerOptions options = QuestionAskerOptions.parse(args);
        GlobalProperties.setDebug(options.isDebug());
        if(options.getProperties() != null) GlobalProperties.loadProperties(options.getProperties());

        qt.setAvoidPronounsAndDemonstratives(options.isDropPro());
        trans.setDoPronounNPC(options.isDoPronounNPC());
        trans.setDoNonPronounNPC(options.isDoNonPronounNPC());

        if (options.getModelPath() != null) {
            System.err.println("Loading question ranking models from " + options.getModelPath() + "...");
            qr = new QuestionRanker();
            qr.loadModel(options.getModelPath());
        }

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

            if (GlobalProperties.getDebug()) System.err.println("\nInput Text:");
            String doc = "What time is it?";


//            while (true) {
//                outputQuestionList.clear();
//                doc = "";
//                buf = "";
//
//                buf = br.readLine();
//                if (buf == null) {
//                    break;
//                }
//                doc += buf;
//
//                while (br.ready()) {
//                    buf = br.readLine();
//                    if (buf == null) {
//                        break;
//                    }
//                    if (buf.matches("^.*\\S.*$")) {
//                        doc += buf + " ";
//                    } else {
//                        doc += "\n";
//                    }
//                }
//                if (doc.length() == 0) {
//                    break;
//                }



                long startTime = System.currentTimeMillis();
                List<String> sentences = AnalysisUtilities.getSentences(doc);

                //iterate over each segmented sentence and generate questions
                List<Tree> inputTrees = new ArrayList<Tree>();

                for (String sentence : sentences) {
                    if (GlobalProperties.getDebug()) System.err.println("Question Asker: sentence: " + sentence);

                    parsed = AnalysisUtilities.getInstance().parseSentence(sentence).parse;
                    inputTrees.add(parsed);
                }

                if (GlobalProperties.getDebug())
                    System.err.println("Seconds Elapsed Parsing:\t" + ((System.currentTimeMillis() - startTime) / 1000.0));

                //step 1 transformations
                List<Question> transformationOutput = trans.transform(inputTrees);

                //step 2 question transducer
                for (Question t : transformationOutput) {
                    if (GlobalProperties.getDebug())
                        System.err.println("Stage 2 Input: " + t.getIntermediateTree().yield().toString());
                    qt.generateQuestionsFromParse(t);
                    outputQuestionList.addAll(qt.getQuestions());
                }

                //remove duplicates
                QuestionTransducer.removeDuplicateQuestions(outputQuestionList);

                //step 3 ranking
                if (qr != null) {
                    qr.scoreGivenQuestions(outputQuestionList);
                    boolean doStemming = true;
                    QuestionRanker.adjustScores(outputQuestionList, inputTrees, options.isAvoidFreqWords(), options.isPreferWH(), options.isDownweightPronouns(), doStemming);
                    QuestionRanker.sortQuestions(outputQuestionList, false);
                }

                //now print the questions
                //double featureValue;
                for (Question question : outputQuestionList) {
                    if (question.getTree().getLeaves().size() > options.getMaxLength()) {
                        continue;
                    }
                    if (options.isJustWH() && question.getFeatureValue("whQuestion") != 1.0) {
                        continue;
                    }
                    System.out.print(question.yield());
                    if (options.isPrintVerbose())
                        System.out.print("\t" + AnalysisUtilities.getCleanedUpYield(question.getSourceTree()));
                    Tree ansTree = question.getAnswerPhraseTree();
                    if (options.isPrintVerbose()) System.out.print("\t");
                    if (ansTree != null) {
                        if (options.isPrintVerbose())
                            System.out.print(AnalysisUtilities.getCleanedUpYield(question.getAnswerPhraseTree()));
                    }
                    if (options.isPrintVerbose()) System.out.print("\t" + question.getScore());
                    //System.err.println("Answer depth: "+question.getFeatureValue("answerDepth"));

                    System.out.println();
                }

                if (GlobalProperties.getDebug())
                    System.err.println("Seconds Elapsed Total:\t" + ((System.currentTimeMillis() - startTime) / 1000.0));
                //prompt for another piece of input text
                if (GlobalProperties.getDebug()) System.err.println("\nInput Text:");
//            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void printFeatureNames() {
        List<String> featureNames = Question.getFeatureNames();
        for (int i = 0; i < featureNames.size(); i++) {
            if (i > 0) {
                System.out.print("\n");
            }
            System.out.print(featureNames.get(i));
        }
        System.out.println();
    }

}

package de.fosd.typechef.lexer.options;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class Options {

    public static class OptionGroup implements Comparable {
        private int priority;
        private String name;
        private List<Option> options;

        public OptionGroup(String name, int priority, Option... options) {
            this.name = name;
            this.options = Arrays.asList(options);
            this.priority = priority;
        }

        @Override
        public int compareTo(Object o) {
            if (o instanceof OptionGroup)
                if (((OptionGroup) o).priority < this.priority) return 1;
                else return -1;
            return 0;
        }
    }

    public static class Option extends LongOpt {
        private String eg;
        private String help;

        public Option(String word, int arg, int ch, String eg, String help) {
            super(word, arg, null, ch);
            this.eg = eg;
            this.help = help;
        }
    }

    protected List<OptionGroup> getOptionGroups() {
        return new ArrayList<OptionGroup>();
    }

    private Option[] getOptions(List<OptionGroup> og) {
        ArrayList<Option> r = new ArrayList<Option>();
        for (OptionGroup g : og)
            r.addAll(g.options);
        return r.toArray(new Option[0]);
    }

    public void parseOptions(String[] args) throws OptionException {
        Option[] opts = getOptions(getOptionGroups());
        String sopts = getShortOpts(opts);
        Getopt g = new Getopt("TypeChef", args, sopts, opts);
        int c;
        while ((c = g.getopt()) != -1) {
            if (!interpretOption(c, g))
                throw new OptionException("Illegal option " + (char) c);
        }

        for (int i = g.getOptind(); i < args.length; i++) {
            String f = args[i];
            if (!new File(f).exists())
                throw new OptionException("File not found " + f);
            files.add(f);
        }

        afterParsing();
    }

    protected void afterParsing() throws OptionException {
    }

    protected boolean interpretOption(int c, Getopt g) throws OptionException {
        return false;
    }


    private String getShortOpts(Option[] opts) throws OptionException {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < opts.length; i++) {
            char c = (char) opts[i].getVal();
            if (!Character.isLetterOrDigit(c))
                continue;
            for (int j = 0; j < buf.length(); j++)
                if (buf.charAt(j) == c)
                    throw new OptionException("Duplicate short option " + c + " with " + opts[i].getName());
            buf.append(c);
            switch (opts[i].getHasArg()) {
                case LongOpt.NO_ARGUMENT:
                    break;
                case LongOpt.OPTIONAL_ARGUMENT:
                    buf.append("::");
                    break;
                case LongOpt.REQUIRED_ARGUMENT:
                    buf.append(":");
                    break;
            }
        }
        return buf.toString();
    }


    @SuppressWarnings("unchecked")
    void printUsage() {
        StringBuilder text = new StringBuilder("Parameters: \n");
        List<OptionGroup> og = getOptionGroups();
        Collections.sort(og);
        for (OptionGroup g : og) {
            text.append("\n  ").append(g.name).append("\n");
            for (Option opt : g.options) {
                StringBuilder line = new StringBuilder();
                line.append("    --").append(opt.getName());
                switch (opt.getHasArg()) {
                    case LongOpt.NO_ARGUMENT:
                        break;
                    case LongOpt.OPTIONAL_ARGUMENT:
                        line.append("[=").append(opt.eg).append(']');
                        break;
                    case LongOpt.REQUIRED_ARGUMENT:
                        line.append('=').append(opt.eg);
                        break;
                }
                if (Character.isLetterOrDigit(opt.getVal()))
                    line.append(" (-").append((char) opt.getVal()).append(")");
                if (line.length() < 35) {
                    while (line.length() < 35)
                        line.append(' ');
                } else {
                    line.append('\n');
                    for (int j = 0; j < 35; j++)
                        line.append(' ');
                }
                /* This should use wrap. */
                line.append(opt.help);
                line.append('\n');
                text.append(line);
            }
        }

        System.out.println(text);
    }


    protected List<String> files = new ArrayList<String>();

    public List<String> getFiles() {
        return files;
    }

    protected void checkFileExists(String file) throws OptionException {
        File f = new File(file);
        if (!(f.exists() && f.isFile()))
            throw new OptionException("Expected a file, found " + file);
    }

    protected void checkDirectoryExists(String file) throws OptionException {
        File f = new File(file);
        if (!(f.exists() && f.isDirectory()))
            throw new OptionException("Expected a directory, found " + file);
    }


}
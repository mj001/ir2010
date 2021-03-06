package ifs.ir.io;

import ifs.ir.NGramResult;
import ifs.ir.ParsedFile;
import weka.core.stemmers.LovinsStemmer;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

public class Reader {
    private double upper_bound;
    private double lower_bound;
    private boolean stemming;
    private ArrayList<String> files;

    public Reader() {
        create(0, -1);
    }

    public Reader(double lower_bound, double upper_bound) {
        create(lower_bound, upper_bound);
    }

    private void create(double lower_bound, double upper_bound) {
        this.lower_bound = lower_bound;
        this.upper_bound = upper_bound;
    }

    public NGramResult readFromDirectory(String dir, int n) throws IOException {
        ArrayList<ParsedFile> file_al = new ArrayList<ParsedFile>();

        HashMap<String, Integer> hm = new HashMap<String, Integer>();
        files = new ArrayList<String>();
        search_for_files(dir);

        int z = 0;
        for (String file : files) {
            ArrayList<HashMap<String, Integer>> tmp = readFromFile(file, n, hm);
            hm = tmp.get(0);
            ParsedFile pf = new ParsedFile(get_klass(file), tmp.get(1));

            file_al.add(pf);

//            if (++z > 800)
//                break;
        }

        return new NGramResult(filter(hm), file_al);
    }


    private String get_klass(String fullpath) {
        int lastslash = fullpath.lastIndexOf("/");
        int slashbefore = 0;

        for (int i = 0; i < lastslash; ++i) {
            if (fullpath.charAt(i) == '/')
                slashbefore = i + 1;
        }
        
        return fullpath.substring(slashbefore, lastslash);
    }

    private void search_for_files(String path) {
        File f = new File(path);

        for (File file : f.listFiles()) {
            if (file.isDirectory())
                search_for_files(file.getPath());
            else
                files.add(file.getPath());
        }
    }

    private HashMap<String, Integer> filter(HashMap<String, Integer> al) {
        int total = sum_ngrams_up(al);

        HashMap<String, Integer> a = (HashMap<String, Integer>) al.clone();

        for (Map.Entry<String, Integer> entry : a.entrySet()) {
            if (entry.getValue() * 100.0 / total < lower_bound || (upper_bound > 0 && entry.getValue() * 100.0 / total >= upper_bound))
                al.remove(entry.getKey());
        }
        return al;
    }

  
    private int sum_ngrams_up(HashMap<String, Integer> al) {
        int ret = 0;

        for (Integer ng : al.values()) {
            ret += ng;
        }
        return ret;
    }

    private ArrayList<HashMap<String, Integer>> readFromFile(String file, int n, HashMap<String, Integer> ng) throws IOException {
        InputStream stream = new BufferedInputStream(new FileInputStream(file));
        return read(stream, n, ng);
    }

    private ArrayList<HashMap<String, Integer>> read(InputStream stream, int n, HashMap<String, Integer> ng)
            throws IOException {

        ArrayList<HashMap<String, Integer>> ret = new ArrayList<HashMap<String, Integer>>();
        HashMap<String, Integer> file = new HashMap<String, Integer>();

        byte ba[] = new byte[n];
        ba[n - 1] = 42;
        int i = 0;

        if (stemming) {
            StringBuilder sb = read_file(stream);
            StringTokenizer st = new StringTokenizer(sb.toString());

            for (byte b : stem_and_convert_to_bytes(sb, st))
                i = process(n, ng, file, b, ba, i);
        } else {
            BufferedInputStream bi = new BufferedInputStream(stream);

            int b;
            while ((b = bi.read()) != -1) {
                i = process(n, ng, file, b, ba, i);
            }
        }

        ret.add(ng);
        ret.add(file);

        return ret;
    }

    private byte[] stem_and_convert_to_bytes(StringBuilder sb, StringTokenizer st) {
        byte[] bytes = new byte[sb.toString().length()];
        int counter = 0;

        while (st.hasMoreTokens()) {
            String tmp = st.nextToken();
            LovinsStemmer stemmer = new LovinsStemmer();

            String tmp2 = stemmer.stem(tmp);
            for (int z = 0; z < tmp2.length(); z++) {
                bytes[counter] = (byte) tmp2.charAt(z);
                counter++;
            }
        }
        return bytes;
    }

    private StringBuilder read_file(InputStream stream) throws IOException {
        InputStreamReader converter = new InputStreamReader(stream);
        BufferedReader in = new BufferedReader(converter);

        String line;
        StringBuilder sb = new StringBuilder();
        while ((line = in.readLine()) != null) {
            sb.append(line);
        }
        return sb;
    }

    private int process(int n, HashMap<String, Integer> count, HashMap<String, Integer> file, int b, byte[] ba, int i) {
        if (b == 13 || b == 10 || b == 9)
            b = 32;
        i++;

        if (i > n && b != 32) {
            System.arraycopy(ba, 1, ba, 0, n - 1);
            ba[n - 1] = (byte) b;
            newNGram(count, ba);
            newNGram(file, ba);
        }
        return i;
    }

    protected void newNGram(HashMap<String, Integer> count, byte[] ba) {

        String ng = bytes_to_string(ba);
        Integer cng = count.get(ng);

        if (cng != null) {
            count.put(ng, cng + 1);
        } else
            count.put(ng, 1);
    }

    private String bytes_to_string(byte[] ba) {
        StringBuffer sb = new StringBuffer();
        for (byte b : ba) {
            sb.append((char) b);
        }

        return sb.toString();
    }

    public void setStemming(boolean stemming) {
        this.stemming = stemming;
    }
}
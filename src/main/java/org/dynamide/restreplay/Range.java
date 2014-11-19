package org.dynamide.restreplay;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.*;

public class Range {
    public static class RangeTupple{
        public int min = 0;
        public int max = -1;
        public RangeTupple(int oneval){
            this.min = oneval;
            this.max = -1;
        }
        public RangeTupple(int min, int max){
            this.min = min;
            this.max = max;
        }
        public RangeTupple(String smin) {
            this(smin, "-1");
        }
        public RangeTupple(String smin, String smax) {
            if (smin.toUpperCase().indexOf('X') > -1){
                char series = smin.charAt(0);
                smin = ""+series+"00";
                smax = ""+series+"99";
            }
            this.min = Integer.parseInt(smin);
            this.max = Integer.parseInt(smax);
        }
        public boolean valueInRange(int val){
            if (max == -1){  //not a range, but a single value.  Must equal the value stored in min.
                return (val == min);
            } else {
                return (min <= val && val <= max);
            }
        }
    }

    public Range(String text) {
        if (isValidIntRangeInput(text)){
            regexString = text;
            Pattern re_next_val = Pattern.compile(
                    "# extract next integers/integer range value.    \n" +
                            "([0-9xX]+)      # $1: 1st integer (Base).         \n" +
                            "(?:           # Range for value (optional).     \n" +
                            "  -           # Dash separates range integer.   \n" +
                            "  ([0-9]+)    # $2: 2nd integer (Range)         \n" +
                            ")?            # Range for value (optional). \n" +
                            "(?:,|$)       # End on comma or string end.",
                    Pattern.COMMENTS
            );
            Matcher m = re_next_val.matcher(text);
            while (m.find()) {
                String smin = m.group(1);
                if (m.group(2) != null) {
                    String smax = m.group(2);
                    RangeTupple tupple = new RangeTupple(smin, smax);
                    ranges.add(tupple);
                } else {
                    RangeTupple tupple = new RangeTupple(smin);
                    ranges.add(tupple);
                }
            }
        }
        //if ( ! ("["+regexString+"]").equals(this.toString())){
        //    throw new IllegalArgumentException("Not a valid range: "+regexString);
        //}
    }

    private String regexString = "";

    private List<RangeTupple> ranges = new ArrayList<RangeTupple>();

    public String toString(){
        StringBuffer b = new StringBuffer();
        int i = 0;
        for (RangeTupple tupple: ranges){
            if (i>0){
                b.append(',');
            }
            i++;
            if (tupple.max == -1) {  //not a range, but a single value.  Must equal the value stored in min.
                b.append(tupple.min);
            } else {
                b.append(tupple.min).append('-').append(tupple.max);
            }

        }
        return "["+b+"]";
    }

     public boolean valueInRange(String text) {
        try {
            return valueInRange(Integer.parseInt(text));
        } catch (NumberFormatException e){
            return false;
        }
    }

    public boolean valueInRange(int val){
        for (RangeTupple tupple: ranges){
            if (tupple.valueInRange(val)) {
                return true;
            }
        }
        return false;
    }


    //===========static=========================================================

    public static Boolean isValidIntRangeInput(String text) {
        Pattern re_valid = Pattern.compile(
                "# Validate comma separated integers/integer ranges.\n" +
                        "^             # Anchor to start of string.         \n" +
                        "[0-9xX]+        # Integer of 1st value (required).   \n" +
                        "(?:           # Range for 1st value (optional).    \n" +
                        "  -           # Dash separates range integer.      \n" +
                        "  [0-9]+      # Range integer of 1st value.        \n" +
                        ")?            # Range for 1st value (optional).    \n" +
                        "(?:           # Zero or more additional values.    \n" +
                        "  ,           # Comma separates additional values. \n" +
                        "  [0-9xX]+      # Integer of extra value (required). \n" +
                        "  (?:         # Range for extra value (optional).  \n" +
                        "    -         # Dash separates range integer.      \n" +
                        "    [0-9]+    # Range integer of extra value.      \n" +
                        "  )?          # Range for extra value (optional).  \n" +
                        ")*            # Zero or more additional values.    \n" +
                        "$             # Anchor to end of string.           ",
                Pattern.COMMENTS
        );
        Matcher m = re_valid.matcher(text);
        if (m.matches()) return true;
        else return false;
    }

    //TODO: convert this to a unit  test.  For now, just run and check the output for rules.
    public static void main(String[] args) {
        String[] arr = new String[]
                { // Valid inputs:
                        "100",
                        "1xx",
                        "2xx",
                        "3xx",
                        "2xx",
                        "4xx",
                        "5xx",
                        "100,200,300",
                        "100-400",
                        "100-201,300-399,404-499",
                        // Invalid inputs:
                        "A",
                        "100,200,",
                        "100 - 200",
                        " ",
                        ""
                };

        String[] arrCodes = new String[]
                { // Valid inputs:
                        "100",
                        "201",
                        "200",
                        "300",
                        "302",
                        "400",
                        "404",
                        "499",
                        "500",
                        "501",
                        // Invalid inputs:
                        "A",
                        "1,2,",
                        "1 - 9",
                        " ",
                        ""
                };
        // Loop through all test input strings:
        int i = 0;
        for (String s : arr) {
            String msg = "String[" + ++i + "] = \"" + s + "\" is ";
            if (isValidIntRangeInput(s)) {
                // Valid input line
                System.out.println(msg + "valid input. Parsing...");
                Range range = new Range(s);

                for (String sCode: arrCodes){
                    System.out.println(""+sCode +" is in range "+range+" : "+range.valueInRange(sCode));
                }
            } else {
                // Match attempt failed
                System.out.println(msg + "NOT valid input.");
            }
        }
    }
}

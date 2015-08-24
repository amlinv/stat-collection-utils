/*
 *   Copyright 2015 AML Innovation & Consulting LLC
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.amlinv.jmxutil.polling;

import com.amlinv.jmxutil.MBeanLocationParameterSource;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Replacer of parameters in strings given the original string and the source of parameter values.
 *
 * Created by art on 8/22/15.
 */
public class ParameterReplacer {
    private final Pattern replaceParamPattern = Pattern.compile("\\$\\{(?<paramName>[^}]*)\\}");

    /**
     * Given a string potentially containing one or more copies of the replacement pattern, ${name}, where name may be
     * any value identifier (e.g. home_dir1), replace each occurrence with the value of the parameter with the enclosed
     * name.
     *
     * @param pattern string potentially containing one or more copies of the replacement pattern.
     * @param parameterSource source of values for parameters for replacement into the given pattern.
     * @return resulting string after replacing parameter values for the replacement pattern.
     */
    public String replaceObjectNameParameters (String pattern, MBeanLocationParameterSource parameterSource) {
        Matcher matcher = replaceParamPattern.matcher(pattern);
        StringBuffer result = new StringBuffer();

        while ( matcher.find() ) {
            String name = matcher.group("paramName");
            String value = parameterSource.getParameter(name);

            if ( value != null ) {
                matcher.appendReplacement(result, value);
            } else {
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()));
            }
        }

        matcher.appendTail(result);

        return  result.toString();
    }
}

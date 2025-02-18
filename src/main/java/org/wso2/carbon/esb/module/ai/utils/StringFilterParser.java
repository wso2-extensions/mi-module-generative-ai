/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.esb.module.ai.utils;

import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.FilterParser;
import dev.langchain4j.store.embedding.filter.comparison.*;
import dev.langchain4j.store.embedding.filter.logical.And;
import dev.langchain4j.store.embedding.filter.logical.Not;
import dev.langchain4j.store.embedding.filter.logical.Or;

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringFilterParser implements FilterParser {

    @Override
    public Filter parse(String dsl) throws IllegalArgumentException {
        if (dsl == null || dsl.isEmpty()) {
            return null;
        }
        dsl = dsl.trim();
        if (dsl.startsWith("and(") || dsl.startsWith("or(") || dsl.startsWith("not(")) {
            return parseLogicalFilter(dsl);
        } else {
            return parseComparisonFilter(dsl);
        }
    }

    private Filter parseLogicalFilter(String dsl) throws IllegalArgumentException {
        String operation = dsl.substring(0, dsl.indexOf('('));
        String innerDsl = dsl.substring(dsl.indexOf('(') + 1, dsl.lastIndexOf(')'));
        String[] parts = splitLogicalOperands(innerDsl);

        Filter left = parse(parts[0].trim());
        Filter right = parts.length > 1 ? parse(parts[1].trim()) : null;

        return switch (operation) {
            case "and" -> new And(left, Objects.requireNonNull(right));
            case "or" -> new Or(left, Objects.requireNonNull(right));
            case "not" -> {
                if (right != null) {
                    throw new IllegalArgumentException("Not operation must have exactly one operand");
                }
                yield new Not(left);
            }
            default -> throw new IllegalArgumentException("Unknown logical operation: " + operation);
        };
    }

    private static String[] splitLogicalOperands(String innerDsl) {
        int bracketCount = 0;
        int splitIndex = -1;

        for (int i = 0; i < innerDsl.length(); i++) {
            char c = innerDsl.charAt(i);
            if (c == '(') bracketCount++;
            if (c == ')') bracketCount--;
            if (c == ',' && bracketCount == 0) {
                splitIndex = i;
                break;
            }
        }

        if (splitIndex == -1) {
            return new String[]{innerDsl};
        } else {
            return new String[]{innerDsl.substring(0, splitIndex), innerDsl.substring(splitIndex + 1)};
        }
    }

    private static Filter parseComparisonFilter(String dsl) throws IllegalArgumentException {
        Pattern pattern = Pattern.compile("metadataKey\\(\"(.*?)\"\\)\\.(.*?)\\((.*?)\\)");
        Matcher matcher = pattern.matcher(dsl);
        if (matcher.find()) {
            String key = matcher.group(1);
            String operation = matcher.group(2);
            String value = matcher.group(3).replace("\"", "");

            return switch (operation) {
                case "isEqualTo" -> new IsEqualTo(key, value);
                case "isNotEqualTo" -> new IsNotEqualTo(key, value);
                case "isGreaterThan" -> new IsGreaterThan(key, value);
                case "isGreaterThanOrEqualTo" -> new IsGreaterThanOrEqualTo(key, value);
                case "isLessThan" -> new IsLessThan(key, value);
                case "isLessThanOrEqualTo" -> new IsLessThanOrEqualTo(key, value);
                case "isIn" -> new IsIn(key, Arrays.asList(value.split(",")));
                case "isNotIn" -> new IsNotIn(key, Arrays.asList(value.split(",")));
                default -> throw new IllegalArgumentException("Unknown comparison operation: " + operation);
            };
        }
        throw new IllegalArgumentException("Invalid DSL format");
    }
}

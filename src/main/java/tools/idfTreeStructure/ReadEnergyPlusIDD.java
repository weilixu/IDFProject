package main.java.tools.idfTreeStructure;

import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ReadEnergyPlusIDD {
    private final Logger LOG = LoggerFactory.getLogger(ReadEnergyPlusIDD.class);

    public static void main(String[] args) {
        ReadEnergyPlusIDD test = new ReadEnergyPlusIDD();
        test.readIDDGenExcel("/Users/weilixu/Documents/GitHub/BuildSimHub/resource/idd_v9.4",
                "/Users/weilixu/Documents/BuildSimHub/TreeStructure/energyplus tree - v9.4.xlsx",
                "/Users/weilixu/Documents/BuildSimHub/TreeStructure/v9.0 First Level.txt");
    }

    private Map<String, String> getFirstLevel(String firstLevelPath) {
        Map<String, String> res = new HashMap<>();

        String line;
        String firstLevel = null;
        try (FileInputStream fis = new FileInputStream(new File(firstLevelPath));
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(isr)) {
            while ((line = br.readLine()) != null) {
                if (!line.matches("^\\s+.*")) {
                    firstLevel = line.trim();
                } else {
                    res.put(line.trim(), firstLevel);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return res;
    }

    private Map<String, Map<String, String>> getSplitted() {
        Map<String, Map<String, String>> res = new HashMap<>();

        Map<String, String> economics = new HashMap<>();
        economics.put("CurrencyType", "Component Cost");
        economics.put("ComponentCost:Adjustments", "Component Cost");
        economics.put("ComponentCost:Reference", "Component Cost");
        economics.put("ComponentCost:LineItem", "Component Cost");
        economics.put("UtilityCost:Tariff", "Utility tariff");
        economics.put("UtilityCost:Qualify", "Utility tariff");
        economics.put("UtilityCost:Charge:Simple", "Utility tariff");
        economics.put("UtilityCost:Charge:Block", "Utility tariff");
        economics.put("UtilityCost:Ratchet", "Utility tariff");
        economics.put("UtilityCost:Variable", "Utility tariff");
        economics.put("UtilityCost:Computation", "Utility tariff");
        economics.put("LifeCycleCost:Parameters", "Life cycle cost");
        economics.put("LifeCycleCost:RecurringCosts", "Life cycle cost");
        economics.put("LifeCycleCost:NonrecurringCost", "Life cycle cost");
        economics.put("LifeCycleCost:UsePriceEscalation", "Life cycle cost");
        economics.put("LifeCycleCost:UseAdjustment", "Life cycle cost");
        res.put("Economics", economics);

        return res;
    }

    private Map<String, String> getSplittedFirstLevel() {
        Map<String, String> res = new HashMap<>();
        res.put("Economics", "Economics");

        return res;
    }

    private void readIDDGenExcel(String iddPath, String xlsxPath, String firstLevelPath) {
        XSSFWorkbook xlsx = new XSSFWorkbook();
        XSSFSheet sheet = xlsx.createSheet();

        int rowNum = 0;
        XSSFRow row = sheet.createRow(rowNum++);

        Map<String, String> map = getFirstLevel(firstLevelPath);
        Map<String, Map<String, String>> splits = getSplitted();
        Map<String, String> splitsFirstLevel = getSplittedFirstLevel();

        String line;
        String group;
        LinkedHashMap<String, LinkedHashMap<String, ArrayList<String>>> treeMap = new LinkedHashMap<>();
        ArrayList<String> cur = null;
        Map<String, String> labelToGroup = null;
        boolean checkSplit = false;

        try (FileInputStream fis = new FileInputStream(new File(iddPath));
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(isr)) {
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                if (line.startsWith("\\group")) {
                    checkSplit = false;

                    group = line.substring(line.indexOf(" ") + 1);
                    row.createCell(1).setCellValue(group);

                    String firstLevel = map.get(group);
                    if (firstLevel == null) {
                        if (splits.containsKey(group)) {
                            labelToGroup = splits.get(group);

                            firstLevel = splitsFirstLevel.get(group);
                            checkSplit = true;
                        } else {
                            System.err.println(group + " don't have first level");
                            System.exit(1);
                        }
                    }

                    if (!treeMap.containsKey(firstLevel)) {
                        LinkedHashMap<String, ArrayList<String>> subMap = new LinkedHashMap<>();
                        treeMap.put(firstLevel, subMap);
                    }

                    if (!checkSplit) {
                        LinkedHashMap<String, ArrayList<String>> subMap = treeMap.get(firstLevel);
                        if (!subMap.containsKey(group)) {
                            ArrayList<String> labels = new ArrayList<>();
                            subMap.put(group, labels);
                        }

                        cur = subMap.get(group);
                    }
                } else if (line.endsWith(",") && !line.startsWith("!") && !line.startsWith("\\")) {
                    String label = line.substring(0, line.length() - 1);
                    if (label.contains(",")) {
                        continue;
                    }

                    if (checkSplit) {
                        String sGroup = labelToGroup.get(label);
                        if (sGroup == null) {
                            System.err.println(label + " don't have split group");
                            System.exit(1);
                        }

                        String firstLevel = map.get(sGroup);
                        if (firstLevel == null) {
                            System.err.println(sGroup + " don't have split first level");
                            System.exit(1);
                        }

                        if (!treeMap.containsKey(firstLevel)) {
                            LinkedHashMap<String, ArrayList<String>> subMap = new LinkedHashMap<>();
                            treeMap.put(firstLevel, subMap);
                        }

                        LinkedHashMap<String, ArrayList<String>> subMap = treeMap.get(firstLevel);
                        if (!subMap.containsKey(sGroup)) {
                            ArrayList<String> labels = new ArrayList<>();
                            subMap.put(sGroup, labels);
                        }

                        subMap.get(sGroup).add(label);
                    } else {
                        cur.add(label);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Write to Xlsx
        for (String firstLevel : treeMap.keySet()) {
            row.createCell(0).setCellValue(firstLevel);
            LinkedHashMap<String, ArrayList<String>> subMap = treeMap.get(firstLevel);

            for (String g : subMap.keySet()) {
                row.createCell(1).setCellValue(g);

                for (String label : subMap.get(g)) {
                    row.createCell(2).setCellValue(label);
                    row = sheet.createRow(rowNum++);
                }
            }
        }

        try (FileOutputStream fos = new FileOutputStream(xlsxPath)) {
            xlsx.write(fos);
            fos.flush();
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }

        try {
            xlsx.close();
        } catch (IOException ignored) {}
    }
}

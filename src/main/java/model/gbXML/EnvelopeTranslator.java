package main.java.model.gbXML;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import main.java.util.NumUtil;
import org.jdom2.Element;
import org.jdom2.Namespace;

import main.java.model.idd.EnergyPlusFieldTemplate;
import main.java.model.idd.EnergyPlusObjectTemplate;
import main.java.model.idd.IDDParser;
import main.java.model.idf.IDFFileObject;
import main.java.model.idf.IDFObject;

public class EnvelopeTranslator {

	private HashMap<String, String> bs_idToObjectMap;
	private Namespace ns;
	private IDDParser iddParser;

	public EnvelopeTranslator(Namespace ns, IDDParser parser) {
		bs_idToObjectMap = new HashMap<String, String>();
		this.ns = ns;
		this.iddParser = parser;
	}

	public String getObjectName(String id) {
		return bs_idToObjectMap.get(id);
	}

	public IDFObject translateMaterial(Element materialElement) {
		IDFObject translatedMaterial = null;

		// only take the immediate child element
		Element nameElement = materialElement.getChild("Name", ns);
		Element thicknessElement = materialElement.getChild("Thickness", ns);
		Element thermalConductivityElement = materialElement.getChild("Conductivity", ns);
		Element densityElement = materialElement.getChild("Density", ns);
		Element specificHeatElement = materialElement.getChild("SpecificHeat", ns);
		Element rValueElement = materialElement.getChild("R-value", ns);

		if (nameElement == null) {
			// TODO ERROR - (Material element 'Name' is empty, material will not
			// be created')
			// stop the process
		}

		String id = materialElement.getAttributeValue("id");
		// add to object map
		String materialName = nameElement.getText();
		bs_idToObjectMap.put(id, materialName);

		materialName = escapeName(id, materialName);
		/*
		 * BuildSimHub Assumptions Material Name - required Roughness - default to
		 * MediumRough Thickness - required, m Conductivity - required, W/m-K Density -
		 * required, kg/m3 Specific Heat - required, J/kg-K Thermal Absorptance, IDD
		 * default of 0.9 Solar Absorptance, IDD default of 0.7 Visible Absorptance, IDD
		 * default of 0.7
		 */

		if (nameElement != null && thicknessElement != null && thermalConductivityElement != null
				&& densityElement != null && specificHeatElement != null) {
			// start process thickness - buildsimhub unit = m
			String thicknessUnit = thicknessElement.getAttributeValue("unit");
			if (thicknessUnit == null) {
				// TODO warning ('thickness unit attribute is empty, assume it
				// is meter')
				thicknessUnit = "Meters";
			}
			Double convertRate = GbXMLUnitConversion.lengthUnitConversionRate(thicknessUnit, "Meters");
			Double thickness = stringToDouble(thicknessElement.getText()) * convertRate;

			// start process conductivity - buildsimhub unit = W/m-K
			String conductivityUnit = thermalConductivityElement.getAttributeValue("unit");
			if (conductivityUnit == null) {
				// TODO warning ("thermal conductivity unit attribute is empty,
				// assume it is W/m-k");
				conductivityUnit = "WPerMeterK";
			}

			convertRate = GbXMLUnitConversion.conductivityUnitConversionRate(conductivityUnit, "WPerMeterK");
			Double conductivity = stringToDouble(thermalConductivityElement.getText()) * convertRate;

			// start process density - buildsimhub unit = kg/m3
			String densityUnit = densityElement.getAttributeValue("unit");
			if (densityUnit == null) {
				// TODO warning ("density unit attribute is empty, assume it is
				// kg/m3")
				densityUnit = "KgPerCubicM";
			}

			convertRate = GbXMLUnitConversion.densityUnitConversionRate(densityUnit, "KgPerCubicM");
			Double density = stringToDouble(densityElement.getText()) * convertRate;

			// start process specific heat - buildsimhub unit = J/kg-K
			String specificHeatUnit = specificHeatElement.getAttributeValue("unit");
			if (specificHeatUnit == null) {
				// TODO warning ("specific heat unit attribute is empty, assume
				// it is J/kg-K"
				specificHeatUnit = "JPerKgK";
			}

			convertRate = GbXMLUnitConversion.specificHeatConversion(specificHeatUnit, "JPerKgK");
			Double specificHeat = stringToDouble(specificHeatElement.getText()) * convertRate;

			EnergyPlusObjectTemplate material = iddParser.getObject("Material");
			IDFObject materialObj = new IDFObject("Material", material.getNumberOfMinFields() + 1);
			materialObj.setTopComments(new String[] { "!- Generated by BuildSimHub" });
			for (int i = 0; i < material.getNumberOfMinFields(); i++) {
				String fieldName = material.getFieldTemplateByIndex(i).getFieldName();
				materialObj.setIndexedStandardComment(i, fieldName);
				if (fieldName.equals("Name")) {
					materialObj.setIndexedData(i, materialName);
				} else if (fieldName.equals("Roughness")) {
					materialObj.setIndexedData(i, "MediumRough");
				} else if (fieldName.equals("Thickness")) {
					thickness = resetValue(material.getFieldTemplateByIndex(i), thickness);
					materialObj.setIndexedData(i, thickness.toString(), "m");
				} else if (fieldName.equals("Conductivity")) {
					conductivity = resetValue(material.getFieldTemplateByIndex(i), conductivity);
					materialObj.setIndexedData(i, conductivity.toString(), "W/m-k");
				} else if (fieldName.equals("Density")) {
					density = resetValue(material.getFieldTemplateByIndex(i), density);
					materialObj.setIndexedData(i, density.toString(), "kg/m3");
				} else if (fieldName.equals("Specific Heat")) {
					specificHeat = resetValue(material.getFieldTemplateByIndex(i), specificHeat);
					materialObj.setIndexedData(i, specificHeat.toString(), "J/kgK");
				}
			}
			translatedMaterial = materialObj;

		} else if (rValueElement != null) {
			// Double rvalue = Double.valueOf(rValueElement.getText());
			// start process rvalue heat - buildsimhub unit = J/kg-K
			String thermalResistantUnit = rValueElement.getAttributeValue("unit");
			if (thermalResistantUnit == null) {
				// TODO warning ("thermal resistance unit attribute is empty,
				// assume it is m2/kW"
				thermalResistantUnit = "SquareMeterKPerW";
			}

			Double convertRate = GbXMLUnitConversion.thermalResistantConversion(thermalResistantUnit,
					"SquareMeterKPerW");
			// System.out.println("For r-value: " + convertRate);
			Double thermalResistance = stringToDouble(rValueElement.getText()) * convertRate;

			// the idd specifies a minimum value of 0.001 for rvalue
			EnergyPlusObjectTemplate material = iddParser.getObject("Material:NoMass");
			IDFObject materialObj = new IDFObject("Material:NoMass", material.getNumberOfMinFields() + 1);
			materialObj.setTopComments(new String[] { "!- Generated by BuildSimHub" });
			for (int i = 0; i < material.getNumberOfMinFields(); i++) {
				String fieldName = material.getFieldTemplateByIndex(i).getFieldName();
				materialObj.setIndexedStandardComment(i, fieldName);
				if (fieldName.equals("Name")) {
					materialObj.setIndexedData(i, materialName);
				} else if (fieldName.equals("Roughness")) {
					materialObj.setIndexedData(i, "MediumRough");
				} else if (fieldName.equals("Thermal Resistance")) {
					thermalResistance = resetValue(material.getFieldTemplateByIndex(i), thermalResistance);
					materialObj.setIndexedData(i, thermalResistance.toString(), "m2-K/W");
				}
			}
			translatedMaterial = materialObj;

		} else {
			EnergyPlusObjectTemplate material = iddParser.getObject("Material:NoMass");
			IDFObject materialObj = new IDFObject("Material", material.getNumberOfMinFields() + 1);
			materialObj.setTopComments(new String[] { "!- Generated by BuildSimHub" });
			for (int i = 0; i < material.getNumberOfMinFields(); i++) {
				String fieldName = material.getFieldTemplateByIndex(i).getFieldName();
				materialObj.setIndexedStandardComment(i, fieldName);
				if (fieldName.equals("Name")) {
					materialObj.setIndexedData(i, materialName);
				} else if (fieldName.equals("Roughness")) {
					materialObj.setIndexedData(i, "MediumRough");
				} else if (fieldName.equals("Thermal Resistance")) {
					Double thermalResistance = material.getFieldTemplateByIndex(i).getInclusiveMin();
					materialObj.setIndexedData(i, thermalResistance.toString(), "m2-K/W");
				}
			}
			translatedMaterial = materialObj;
		}
		// done return
		return translatedMaterial;
	}

	public IDFObject translateConstruction(Element element, List<Element> layerElements) {
		// set construction name
		String constructionId = element.getAttributeValue("id");
		String constructionName = escapeName(constructionId, element.getChildText("Name", ns));
		List<String> materialLayer = new ArrayList<String>();
		IDFObject constructionObj = null;
		bs_idToObjectMap.put(constructionId, constructionName);

		// each layers
		Element layerIdElement = element.getChild("LayerId", ns);

		if (layerIdElement == null) {
			return constructionObj;
		}

		String layerId = layerIdElement.getAttributeValue("layerIdRef");
		for (int i = 0; i < layerElements.size(); i++) {
			Element layerElement = layerElements.get(i);
			if (layerId.equals(layerElement.getAttributeValue("id"))) {
				// System.out.println(layerId + " matches " +
				// layerElement.getName());
				List<Element> materialIdElements = layerElement.getChildren("MaterialId", ns);
				// System.out.println(materialIdElements.size());
				for (int j = 0; j < materialIdElements.size(); j++) {
					String materialId = materialIdElements.get(j).getAttributeValue("materialIdRef");
					String materialName = bs_idToObjectMap.get(materialId);

					if (materialName != null) {
						materialLayer.add(materialName);
					}
				}
				break;
			}
		}

		if (!materialLayer.isEmpty()) {
			// object name + object label = 2 fields
			constructionObj = new IDFObject("Construction", 2 + materialLayer.size());
			constructionObj.setTopComments(new String[] { "!- Generated by BuildSimHub" });

			constructionObj.setIndexedStandardComment(0, "Name");
			constructionObj.setIndexedData(0, constructionName);

			int counter = 1;
			if (materialLayer.size() >= counter) {
				// add out layer material
				constructionObj.setIndexedStandardComment(1, "Outside Layer");
				constructionObj.setIndexedData(1, materialLayer.get(0));
				counter++;
			}

			for (int i = 1; i < materialLayer.size(); i++) {
				constructionObj.setIndexedStandardComment(counter, "Layer " + counter);
				constructionObj.setIndexedData(counter, materialLayer.get(i));
				counter++;
			}

		} else {
			return constructionObj;
		}
		return constructionObj;
	}

	/**
	 * This function converts the windowType elements to a simple glazing object in
	 * EnergyPlus The detail Window object is under the development
	 * 
	 * @param element
	 * @return
	 */
	public void translateWindowType(Element element, IDFFileObject file) {
		String windowTypeId = element.getAttributeValue("id");
		String windowName = element.getChildText("Name", ns);
		windowName = escapeName(windowTypeId, windowName);

		// bs_idToObjectMap.put(windowTypeId, windowName);

		// process data
		Double uValue = null;
		Double shgc = null;
		Double tVis = null;

		Element uValueElement = element.getChild("U-value", ns);
		if (uValueElement == null) {
			uValue = 2.4;// TODO WX: change based on climate zone
		} else {
			if (uValueElement.getAttributeValue("unit").equalsIgnoreCase("WPerSquareMeterK")) {
				uValue = Double.parseDouble(uValueElement.getText());
			} else {
				uValue = Double.parseDouble(uValueElement.getText()) * GbXMLUnitConversion
						.thermalResistantConversion(uValueElement.getAttributeValue("unit"), "WPerSquareMeterK");
			}
		}

		Element shgcElement = element.getChild("SolarHeatGainCoeff", ns);
		if (shgcElement == null) {
			shgc = 0.4; // TODO change to baseline based on climate zone
		} else if (shgcElement.getAttributeValue("unit").equalsIgnoreCase("Fraction")) {
			shgc = Double.parseDouble(shgcElement.getText());
		}

		Element transmittanceElement = element.getChild("Transmittance", ns);
		if (transmittanceElement == null) {
			tVis = 0.6;// TODO change to baseline based on climate zone
		} else if (transmittanceElement.getAttributeValue("type").equalsIgnoreCase("Visible")) {
			tVis = Double.parseDouble(transmittanceElement.getText());
		}

		EnergyPlusObjectTemplate windowMaterial = iddParser.getObject("WindowMaterial:SimpleGlazingSystem");
		// this object the minimum field is 3 - exclude visible transmittance
		int numFields = windowMaterial.getNumberOfFields();
		if (tVis == null) {
			numFields -= 1;
		}

		IDFObject windowMaterialObj = new IDFObject("WindowMaterial:SimpleGlazingSystem", numFields + 1);
		windowMaterialObj.setTopComments(new String[] { "!- Generated by BuildSimHub" });
		windowMaterialObj.setIndexedStandardComment(0, "Name");
		windowMaterialObj.setIndexedData(0, windowName);

		windowMaterialObj.setIndexedStandardComment(1, "U-Factor");
		if (uValue != null) {
			windowMaterialObj.setIndexedData(1, uValue.toString(), "W/m2-K");
		}
		
		windowMaterialObj.setIndexedStandardComment(2, "Solar Heat Gain Coefficient");
		if (shgc != null) {
			windowMaterialObj.setIndexedData(2, shgc.toString());
		} else {
			windowMaterialObj.setIndexedData(2, "0.4");
		}

		if (tVis != null) {
			windowMaterialObj.setIndexedStandardComment(3, "Visible Transmittance");
			windowMaterialObj.setIndexedData(3, "0.5");
		}
		file.addObject(windowMaterialObj);

		IDFObject constructionObj = new IDFObject("Construction", 3);
		constructionObj.setTopComments(new String[] { "!- Generated by BuildSimHub" });

		constructionObj.setIndexedStandardComment(0, "Name");
		constructionObj.setIndexedData(0, windowTypeId);
		
		constructionObj.setIndexedStandardComment(1, "Outside Layer");
		constructionObj.setIndexedData(1, windowName);
		
		bs_idToObjectMap.put(windowTypeId, windowTypeId);

		// add the construction
		file.addObject(constructionObj);
	}

	public void setReversedConstruction(String constructionId, String constructionName) {
		bs_idToObjectMap.put(constructionId, constructionName);
	}

	public void addAirConstruction(String surfaceType, String constructionId, IDFFileObject file) {
		Double resistance = 0.0;
		if (surfaceType.equals("Ceiling") || surfaceType.equals("Floor")) {
			resistance = 0.18;
		} else {
			resistance = 0.15;
		}
		
		/*
		 * Material:AirGap,
    F04 Wall air space resistance, !- Name
    0.15;  !- Thermal Resistance {m2-K/W}
		 */
		IDFObject materialAirGap = new IDFObject("Material:AirGap", 3);
		materialAirGap.setTopComments(new String[] { "!- Generated by BuildSimHub" });
		materialAirGap.setIndexedStandardComment(0, "Name");
		materialAirGap.setIndexedData(0, surfaceType + " Air Material");
		
		materialAirGap.setIndexedStandardComment(1, "Thermal Resistance");
		materialAirGap.setIndexedData(1, resistance.toString(),"m2-K/W");
		file.addObject(materialAirGap);
		
		IDFObject construction = new IDFObject("Construction",3);
		construction.setTopComments(new String[] { "!- Generated by BuildSimHub" });
		construction.setIndexedStandardComment(0, "Name");
		construction.setIndexedData(0, "Air " + surfaceType);
		
		construction.setIndexedStandardComment(1, "Outside Layer");
		construction.setIndexedData(1, surfaceType + " Air Material");
		
		file.addObject(construction);

		bs_idToObjectMap.put(constructionId, "Air " + surfaceType);
		// construction

	}

	private Double resetValue(EnergyPlusFieldTemplate template, Double value) {
		Double min = template.getMin();
		if (min == NumUtil.MIN_VALUE) {
			min = template.getInclusiveMin();
		}

		Double max = template.getMax();
		if (max == NumUtil.MAX_VALUE) {
			max = template.getInclusiveMax();
		}

		if (min != NumUtil.MIN_VALUE && value <= min) {
			value = min;
		}

		if (max != NumUtil.MAX_VALUE && value >= max) {
			value = max;
		}
		return value;
	}

	/**
	 * utility function that converts a string to a double value writes error in the
	 * log if the string is not convertable and stop all the process.
	 * 
	 * @param text
	 * @return
	 */
	private Double stringToDouble(String text) {
		Double value = null;
		try {
			value = Double.valueOf(text);
		} catch (NumberFormatException nfe) {
			nfe.printStackTrace();
			// TODO, ERROR 'Value :' + text + 'Cannot convert to double'
		}
		return value;
	}

	private String escapeName(String id, String name) {
		String value = id;
		if (name != null && !name.isEmpty()) {
			value = name;
		}

		return value.replace(",", "-").replace(";", "-");
	}
}

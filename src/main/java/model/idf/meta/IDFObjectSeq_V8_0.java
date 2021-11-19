package main.java.model.idf.meta;

import java.util.HashMap;

public class IDFObjectSeq_V8_0 implements ObjectSeq {
    private static HashMap<String, Integer> objectSeq = null;
    private static HashMap<Integer, String> categorySeq = null;
    
    private static void initObjectSeq(){
        objectSeq = new HashMap<>();

        objectSeq.put("version", 1);
        objectSeq.put("simulationcontrol", 2);
        objectSeq.put("building", 3);
        objectSeq.put("shadowcalculation", 4);
        objectSeq.put("surfaceconvectionalgorithm:inside", 5);
        objectSeq.put("surfaceconvectionalgorithm:outside", 6);
        objectSeq.put("heatbalancealgorithm", 7);
        objectSeq.put("heatbalancesettings:conductionfinitedifference", 8);
        objectSeq.put("zoneairheatbalancealgorithm", 9);
        objectSeq.put("zoneaircontaminantbalance", 10);
        objectSeq.put("zonecapacitancemultiplier:researchspecial", 12);
        objectSeq.put("timestep", 13);
        objectSeq.put("convergencelimits", 14);
        objectSeq.put("programcontrol", 15);
        
        objectSeq.put("compliance:building", 101);
        
        objectSeq.put("site:location", 201);
        objectSeq.put("sizingperiod:designday", 202);
        objectSeq.put("sizingperiod:weatherfiledays", 203);
        objectSeq.put("sizingperiod:weatherfileconditiontype", 204);
        objectSeq.put("runperiod", 205);
        objectSeq.put("runperiod:customrange", 206);
        objectSeq.put("runperiodcontrol:specialdays", 207);
        objectSeq.put("runperiodcontrol:daylightsavingtime", 208);
        objectSeq.put("weatherproperty:skytemperature", 209);
        objectSeq.put("site:weatherstation", 210);
        objectSeq.put("site:heightvariation", 211);
        objectSeq.put("site:groundtemperature:buildingsurface", 212);
        objectSeq.put("site:groundtemperature:fcfactormethod", 213);
        objectSeq.put("site:groundtemperature:shallow", 214);
        objectSeq.put("site:groundtemperature:deep", 215);
        objectSeq.put("site:groundreflectance", 218);
        objectSeq.put("site:groundreflectance:snowmodifier", 219);
        objectSeq.put("site:watermainstemperature", 220);
        objectSeq.put("site:precipitation", 221);
        objectSeq.put("roofirrigation", 222);
        
        objectSeq.put("scheduletypelimits", 301);
        objectSeq.put("schedule:day:hourly", 302);
        objectSeq.put("schedule:day:interval", 303);
        objectSeq.put("schedule:day:list", 304);
        objectSeq.put("schedule:week:daily", 305);
        objectSeq.put("schedule:week:compact", 306);
        objectSeq.put("schedule:year", 307);
        objectSeq.put("schedule:compact", 308);
        objectSeq.put("schedule:constant", 309);
        objectSeq.put("schedule:file", 310);
        
        objectSeq.put("material", 401);
        objectSeq.put("material:nomass", 402);
        objectSeq.put("material:infraredtransparent", 403);
        objectSeq.put("material:airgap", 404);
        objectSeq.put("material:roofvegetation", 405);
        objectSeq.put("windowmaterial:simpleglazingsystem", 406);
        objectSeq.put("windowmaterial:glazing", 407);
        objectSeq.put("windowmaterial:glazinggroup:thermochromic", 408);
        objectSeq.put("windowmaterial:glazing:refractionextinctionmethod", 409);
        objectSeq.put("windowmaterial:gas", 410);
        objectSeq.put("windowgap:supportpillar", 411);
        objectSeq.put("windowgap:deflectionstate", 412);
        objectSeq.put("windowmaterial:gasmixture", 413);
        objectSeq.put("windowmaterial:gap", 414);
        objectSeq.put("windowmaterial:shade", 415);
        objectSeq.put("windowmaterial:complexshade", 416);
        objectSeq.put("windowmaterial:blind", 417);
        objectSeq.put("windowmaterial:screen", 418);
        objectSeq.put("materialproperty:moisturepenetrationdepth:settings", 419);
        objectSeq.put("materialproperty:phasechange", 420);
        objectSeq.put("materialproperty:variablethermalconductivity", 421);
        objectSeq.put("materialproperty:heatandmoisturetransfer:settings", 422);
        objectSeq.put("materialproperty:heatandmoisturetransfer:sorptionisotherm", 423);
        objectSeq.put("materialproperty:heatandmoisturetransfer:suction", 424);
        objectSeq.put("materialproperty:heatandmoisturetransfer:redistribution", 425);
        objectSeq.put("materialproperty:heatandmoisturetransfer:diffusion", 426);
        objectSeq.put("materialproperty:heatandmoisturetransfer:thermalconductivity", 427);
        objectSeq.put("materialproperty:glazingspectraldata", 428);
        objectSeq.put("construction", 429);
        objectSeq.put("construction:cfactorundergroundwall", 430);
        objectSeq.put("construction:ffactorgroundfloor", 431);
        objectSeq.put("construction:internalsource", 432);
        objectSeq.put("windowthermalmodel:params", 433);
        objectSeq.put("construction:complexfenestrationstate", 434);
        objectSeq.put("construction:windowdatafile", 435);
        
        objectSeq.put("globalgeometryrules", 501);
        objectSeq.put("geometrytransform", 502);
        objectSeq.put("zone", 503);
        objectSeq.put("zonelist", 504);
        objectSeq.put("zonegroup", 505);
        objectSeq.put("buildingsurface:detailed", 506);
        objectSeq.put("wall:detailed", 507);
        objectSeq.put("roofceiling:detailed", 508);
        objectSeq.put("floor:detailed", 509);
        objectSeq.put("wall:exterior", 510);
        objectSeq.put("wall:adiabatic", 511);
        objectSeq.put("wall:underground", 512);
        objectSeq.put("wall:interzone", 513);
        objectSeq.put("roof", 514);
        objectSeq.put("ceiling:adiabatic", 515);
        objectSeq.put("ceiling:interzone", 516);
        objectSeq.put("floor:groundcontact", 517);
        objectSeq.put("floor:adiabatic", 518);
        objectSeq.put("floor:interzone", 519);
        objectSeq.put("fenestrationsurface:detailed", 520);
        objectSeq.put("window", 521);
        objectSeq.put("door", 522);
        objectSeq.put("glazeddoor", 523);
        objectSeq.put("window:interzone", 524);
        objectSeq.put("door:interzone", 525);
        objectSeq.put("glazeddoor:interzone", 526);
        objectSeq.put("windowproperty:shadingcontrol", 527);
        objectSeq.put("windowproperty:frameanddivider", 528);
        objectSeq.put("windowproperty:airflowcontrol", 529);
        objectSeq.put("windowproperty:stormwindow", 530);
        objectSeq.put("internalmass", 531);
        objectSeq.put("shading:site", 532);
        objectSeq.put("shading:building", 533);
        objectSeq.put("shading:site:detailed", 534);
        objectSeq.put("shading:building:detailed", 535);
        objectSeq.put("shading:overhang", 536);
        objectSeq.put("shading:overhang:projection", 537);
        objectSeq.put("shading:fin", 538);
        objectSeq.put("shading:fin:projection", 539);
        objectSeq.put("shading:zone:detailed", 540);
        objectSeq.put("shadingproperty:reflectance", 541);
        
        objectSeq.put("surfaceproperty:heattransferalgorithm", 601);
        objectSeq.put("surfaceproperty:heattransferalgorithm:multiplesurface", 602);
        objectSeq.put("surfaceproperty:heattransferalgorithm:surfacelist", 603);
        objectSeq.put("surfaceproperty:heattransferalgorithm:construction", 604);
        objectSeq.put("surfacecontrol:movableinsulation", 605);
        objectSeq.put("surfaceproperty:othersidecoefficients", 606);
        objectSeq.put("surfaceproperty:othersideconditionsmodel", 607);
        objectSeq.put("surfaceconvectionalgorithm:inside:adaptivemodelselections", 608);
        objectSeq.put("surfaceconvectionalgorithm:outside:adaptivemodelselections", 609);
        objectSeq.put("surfaceconvectionalgorithm:inside:usercurve", 610);
        objectSeq.put("surfaceconvectionalgorithm:outside:usercurve", 611);
        objectSeq.put("surfaceproperty:convectioncoefficients", 612);
        objectSeq.put("surfaceproperty:convectioncoefficients:multiplesurface", 613);
        objectSeq.put("surfaceproperties:vaporcoefficients", 614);
        objectSeq.put("surfaceproperty:exteriornaturalventedcavity", 615);
        objectSeq.put("zoneproperty:userviewfactors:bysurfacename", 616);
        
        objectSeq.put("groundheattransfer:control", 701);
        objectSeq.put("groundheattransfer:slab:materials", 702);
        objectSeq.put("groundheattransfer:slab:matlprops", 703);
        objectSeq.put("groundheattransfer:slab:boundconds", 704);
        objectSeq.put("groundheattransfer:slab:bldgprops", 705);
        objectSeq.put("groundheattransfer:slab:insulation", 706);
        objectSeq.put("groundheattransfer:slab:equivalentslab", 707);
        objectSeq.put("groundheattransfer:slab:autogrid", 708);
        objectSeq.put("groundheattransfer:slab:manualgrid", 709);
        objectSeq.put("groundheattransfer:slab:xface", 710);
        objectSeq.put("groundheattransfer:slab:yface", 711);
        objectSeq.put("groundheattransfer:slab:zface", 712);
        objectSeq.put("groundheattransfer:basement:simparameters", 713);
        objectSeq.put("groundheattransfer:basement:matlprops", 714);
        objectSeq.put("groundheattransfer:basement:insulation", 715);
        objectSeq.put("groundheattransfer:basement:surfaceprops", 716);
        objectSeq.put("groundheattransfer:basement:bldgdata", 717);
        objectSeq.put("groundheattransfer:basement:interior", 718);
        objectSeq.put("groundheattransfer:basement:combldg", 719);
        objectSeq.put("groundheattransfer:basement:equivslab", 720);
        objectSeq.put("groundheattransfer:basement:equivautogrid", 721);
        objectSeq.put("groundheattransfer:basement:autogrid", 722);
        objectSeq.put("groundheattransfer:basement:manualgrid", 723);
        objectSeq.put("groundheattransfer:basement:xface", 724);
        objectSeq.put("groundheattransfer:basement:yface", 725);
        objectSeq.put("groundheattransfer:basement:zface", 726);
        
        objectSeq.put("roomairmodeltype", 801);
        objectSeq.put("roomair:temperaturepattern:userdefined", 802);
        objectSeq.put("roomair:temperaturepattern:constantgradient", 803);
        objectSeq.put("roomair:temperaturepattern:twogradient", 804);
        objectSeq.put("roomair:temperaturepattern:nondimensionalheight", 805);
        objectSeq.put("roomair:temperaturepattern:surfacemapping", 806);
        objectSeq.put("roomair:node", 807);
        objectSeq.put("roomairsettings:onenodedisplacementventilation", 808);
        objectSeq.put("roomairsettings:threenodedisplacementventilation", 809);
        objectSeq.put("roomairsettings:crossventilation", 810);
        objectSeq.put("roomairsettings:underfloorairdistributioninterior", 811);
        objectSeq.put("roomairsettings:underfloorairdistributionexterior", 812);
        
        objectSeq.put("people", 901);
        objectSeq.put("comfortviewfactorangles", 902);
        objectSeq.put("lights", 903);
        objectSeq.put("electricequipment", 904);
        objectSeq.put("gasequipment", 905);
        objectSeq.put("hotwaterequipment", 906);
        objectSeq.put("steamequipment", 907);
        objectSeq.put("otherequipment", 908);
        objectSeq.put("zonebaseboard:outdoortemperaturecontrolled", 910);
        objectSeq.put("zonecontaminantsourceandsink:carbondioxide", 912);
        objectSeq.put("zonecontaminantsourceandsink:generic:constant", 913);
        objectSeq.put("surfacecontaminantsourceandsink:generic:pressuredriven", 914);
        objectSeq.put("zonecontaminantsourceandsink:generic:cutoffmodel", 915);
        objectSeq.put("zonecontaminantsourceandsink:generic:decaysource", 916);
        objectSeq.put("surfacecontaminantsourceandsink:generic:boundarylayerdiffusion", 917);
        objectSeq.put("surfacecontaminantsourceandsink:generic:depositionvelocitysink", 918);
        objectSeq.put("zonecontaminantsourceandsink:generic:depositionratesink", 919);
        
        objectSeq.put("daylighting:controls", 1001);
        objectSeq.put("daylighting:delight:controls", 1002);
        objectSeq.put("daylighting:delight:referencepoint", 1003);
        objectSeq.put("daylighting:delight:complexfenestration", 1004);
        objectSeq.put("daylightingdevice:tubular", 1005);
        objectSeq.put("daylightingdevice:shelf", 1006);
        objectSeq.put("daylightingdevice:lightwell", 1007);
        objectSeq.put("output:daylightfactors", 1008);
        objectSeq.put("output:illuminancemap", 1009);
        objectSeq.put("outputcontrol:illuminancemap:style", 1010);
        
        objectSeq.put("zoneinfiltration:designflowrate", 1101);
        objectSeq.put("zoneinfiltration:effectiveleakagearea", 1102);
        objectSeq.put("zoneinfiltration:flowcoefficient", 1103);
        objectSeq.put("zoneventilation:designflowrate", 1104);
        objectSeq.put("zoneventilation:windandstackopenarea", 1105);
        objectSeq.put("zoneairbalance:outdoorair", 1106);
        objectSeq.put("zonemixing", 1107);
        objectSeq.put("zonecrossmixing", 1108);
        objectSeq.put("zonerefrigerationdoormixing", 1109);
        objectSeq.put("zoneearthtube", 1110);
        objectSeq.put("zonecooltower:shower", 1111);
        objectSeq.put("zonethermalchimney", 1112);
        
        objectSeq.put("airflownetwork:simulationcontrol", 1201);
        objectSeq.put("airflownetwork:multizone:zone", 1202);
        objectSeq.put("airflownetwork:multizone:surface", 1203);
        objectSeq.put("airflownetwork:multizone:referencecrackconditions", 1204);
        objectSeq.put("airflownetwork:multizone:surface:crack", 1205);
        objectSeq.put("airflownetwork:multizone:surface:effectiveleakagearea", 1206);
        objectSeq.put("airflownetwork:multizone:component:detailedopening", 1207);
        objectSeq.put("airflownetwork:multizone:component:simpleopening", 1208);
        objectSeq.put("airflownetwork:multizone:component:horizontalopening", 1209);
        objectSeq.put("airflownetwork:multizone:component:zoneexhaustfan", 1210);
        objectSeq.put("airflownetwork:multizone:externalnode", 1211);
        objectSeq.put("airflownetwork:multizone:windpressurecoefficientarray", 1212);
        objectSeq.put("airflownetwork:multizone:windpressurecoefficientvalues", 1213);
        objectSeq.put("airflownetwork:distribution:node", 1214);
        objectSeq.put("airflownetwork:distribution:component:leak", 1215);
        objectSeq.put("airflownetwork:distribution:component:leakageratio", 1216);
        objectSeq.put("airflownetwork:distribution:component:duct", 1217);
        objectSeq.put("airflownetwork:distribution:component:fan", 1218);
        objectSeq.put("airflownetwork:distribution:component:coil", 1219);
        objectSeq.put("airflownetwork:distribution:component:heatexchanger", 1220);
        objectSeq.put("airflownetwork:distribution:component:terminalunit", 1221);
        objectSeq.put("airflownetwork:distribution:component:constantpressuredrop", 1222);
        objectSeq.put("airflownetwork:distribution:linkage", 1223);
        
        objectSeq.put("exterior:lights", 1301);
        objectSeq.put("exterior:fuelequipment", 1302);
        objectSeq.put("exterior:waterequipment", 1303);
        
        objectSeq.put("hvactemplate:thermostat", 1401);
        objectSeq.put("hvactemplate:zone:idealloadsairsystem", 1402);
        objectSeq.put("hvactemplate:zone:fancoil", 1403);
        objectSeq.put("hvactemplate:zone:ptac", 1404);
        objectSeq.put("hvactemplate:zone:pthp", 1405);
        objectSeq.put("hvactemplate:zone:unitary", 1406);
        objectSeq.put("hvactemplate:zone:vav", 1407);
        objectSeq.put("hvactemplate:zone:vav:fanpowered", 1408);
        objectSeq.put("hvactemplate:zone:watertoairheatpump", 1409);
        objectSeq.put("hvactemplate:system:unitary", 1410);
        objectSeq.put("hvactemplate:system:unitaryheatpump:airtoair", 1411);
        objectSeq.put("hvactemplate:system:vav", 1412);
        objectSeq.put("hvactemplate:system:packagedvav", 1413);
        objectSeq.put("hvactemplate:system:dedicatedoutdoorair", 1414);
        objectSeq.put("hvactemplate:plant:chilledwaterloop", 1415);
        objectSeq.put("hvactemplate:plant:chiller", 1416);
        objectSeq.put("hvactemplate:plant:chiller:objectreference", 1417);
        objectSeq.put("hvactemplate:plant:tower", 1418);
        objectSeq.put("hvactemplate:plant:tower:objectreference", 1419);
        objectSeq.put("hvactemplate:plant:hotwaterloop", 1420);
        objectSeq.put("hvactemplate:plant:boiler", 1421);
        objectSeq.put("hvactemplate:plant:boiler:objectreference", 1422);
        objectSeq.put("hvactemplate:plant:mixedwaterloop", 1423);
        
        objectSeq.put("designspecification:outdoorair", 1501);
        objectSeq.put("designspecification:zoneairdistribution", 1502);
        objectSeq.put("sizing:parameters", 1503);
        objectSeq.put("sizing:zone", 1504);
        objectSeq.put("sizing:system", 1506);
        objectSeq.put("sizing:plant", 1507);
        objectSeq.put("outputcontrol:sizing:style", 1508);
        
        objectSeq.put("zonecontrol:humidistat", 1601);
        objectSeq.put("zonecontrol:thermostat", 1602);
        objectSeq.put("zonecontrol:thermostat:operativetemperature", 1603);
        objectSeq.put("zonecontrol:thermostat:thermalcomfort", 1604);
        objectSeq.put("zonecontrol:thermostat:temperatureandhumidity", 1605);
        objectSeq.put("thermostatsetpoint:singleheating", 1606);
        objectSeq.put("thermostatsetpoint:singlecooling", 1607);
        objectSeq.put("thermostatsetpoint:singleheatingorcooling", 1608);
        objectSeq.put("thermostatsetpoint:dualsetpoint", 1609);
        objectSeq.put("thermostatsetpoint:thermalcomfort:fanger:singleheating", 1610);
        objectSeq.put("thermostatsetpoint:thermalcomfort:fanger:singlecooling", 1611);
        objectSeq.put("thermostatsetpoint:thermalcomfort:fanger:singleheatingorcooling", 1612);
        objectSeq.put("thermostatsetpoint:thermalcomfort:fanger:dualsetpoint", 1613);
        objectSeq.put("zonecontrol:contaminantcontroller", 1614);
        
        objectSeq.put("zonehvac:idealloadsairsystem", 1701);
        objectSeq.put("zonehvac:fourpipefancoil", 1702);
        objectSeq.put("zonehvac:windowairconditioner", 1703);
        objectSeq.put("zonehvac:packagedterminalairconditioner", 1704);
        objectSeq.put("zonehvac:packagedterminalheatpump", 1705);
        objectSeq.put("zonehvac:watertoairheatpump", 1706);
        objectSeq.put("zonehvac:dehumidifier:dx", 1707);
        objectSeq.put("zonehvac:energyrecoveryventilator", 1708);
        objectSeq.put("zonehvac:energyrecoveryventilator:controller", 1709);
        objectSeq.put("zonehvac:unitventilator", 1710);
        objectSeq.put("zonehvac:unitheater", 1711);
        objectSeq.put("zonehvac:outdoorairunit", 1712);
        objectSeq.put("zonehvac:outdoorairunit:equipmentlist", 1713);
        objectSeq.put("zonehvac:terminalunit:variablerefrigerantflow", 1714);
        
        objectSeq.put("zonehvac:baseboard:radiantconvective:water", 1801);
        objectSeq.put("zonehvac:baseboard:radiantconvective:steam", 1802);
        objectSeq.put("zonehvac:baseboard:radiantconvective:electric", 1803);
        objectSeq.put("zonehvac:baseboard:convective:water", 1804);
        objectSeq.put("zonehvac:baseboard:convective:electric", 1805);
        objectSeq.put("zonehvac:lowtemperatureradiant:variableflow", 1806);
        objectSeq.put("zonehvac:lowtemperatureradiant:constantflow", 1807);
        objectSeq.put("zonehvac:lowtemperatureradiant:electric", 1808);
        objectSeq.put("zonehvac:lowtemperatureradiant:surfacegroup", 1809);
        objectSeq.put("zonehvac:hightemperatureradiant", 1810);
        objectSeq.put("zonehvac:ventilatedslab", 1811);
        objectSeq.put("zonehvac:ventilatedslab:slabgroup", 1812);
        
        objectSeq.put("airterminal:singleduct:uncontrolled", 1901);
        objectSeq.put("airterminal:singleduct:constantvolume:reheat", 1902);
        objectSeq.put("airterminal:singleduct:vav:noreheat", 1903);
        objectSeq.put("airterminal:singleduct:vav:reheat", 1904);
        objectSeq.put("airterminal:singleduct:vav:reheat:variablespeedfan", 1905);
        objectSeq.put("airterminal:singleduct:vav:heatandcool:noreheat", 1906);
        objectSeq.put("airterminal:singleduct:vav:heatandcool:reheat", 1907);
        objectSeq.put("airterminal:singleduct:seriespiu:reheat", 1908);
        objectSeq.put("airterminal:singleduct:parallelpiu:reheat", 1909);
        objectSeq.put("airterminal:singleduct:constantvolume:fourpipeinduction", 1910);
        objectSeq.put("airterminal:singleduct:constantvolume:cooledbeam", 1911);
        objectSeq.put("airterminal:dualduct:constantvolume", 1912);
        objectSeq.put("airterminal:dualduct:vav", 1913);
        objectSeq.put("airterminal:dualduct:vav:outdoorair", 1914);
        objectSeq.put("zonehvac:airdistributionunit", 1915);
        
        objectSeq.put("zonehvac:equipmentlist", 2001);
        objectSeq.put("zonehvac:equipmentconnections", 2002);
        
        objectSeq.put("fan:constantvolume", 2101);
        objectSeq.put("fan:variablevolume", 2102);
        objectSeq.put("fan:onoff", 2103);
        objectSeq.put("fan:zoneexhaust", 2104);
        objectSeq.put("fanperformance:nightventilation", 2105);
        objectSeq.put("fan:componentmodel", 2106);
        
        objectSeq.put("coil:cooling:water", 2201);
        objectSeq.put("coil:cooling:water:detailedgeometry", 2202);
        objectSeq.put("coil:cooling:dx:singlespeed", 2203);
        objectSeq.put("coil:cooling:dx:twospeed", 2204);
        objectSeq.put("coil:cooling:dx:multispeed", 2205);
        objectSeq.put("coil:cooling:dx:variablespeed", 2206);
        objectSeq.put("coil:cooling:dx:twostagewithhumiditycontrolmode", 2207);
        objectSeq.put("coilperformance:dx:cooling", 2208);
        objectSeq.put("coil:cooling:dx:variablerefrigerantflow", 2209);
        objectSeq.put("coil:heating:dx:variablerefrigerantflow", 2210);
        objectSeq.put("coil:heating:water", 2211);
        objectSeq.put("coil:heating:steam", 2212);
        objectSeq.put("coil:heating:electric", 2213);
        objectSeq.put("coil:heating:electric:multistage", 2214);
        objectSeq.put("coil:heating:gas", 2215);
        objectSeq.put("coil:heating:gas:multistage", 2216);
        objectSeq.put("coil:heating:desuperheater", 2217);
        objectSeq.put("coil:heating:dx:singlespeed", 2218);
        objectSeq.put("coil:heating:dx:multispeed", 2219);
        objectSeq.put("coil:heating:dx:variablespeed", 2220);
        objectSeq.put("coil:cooling:watertoairheatpump:parameterestimation", 2221);
        objectSeq.put("coil:heating:watertoairheatpump:parameterestimation", 2222);
        objectSeq.put("coil:cooling:watertoairheatpump:equationfit", 2223);
        objectSeq.put("coil:cooling:watertoairheatpump:variablespeedequationfit", 2224);
        objectSeq.put("coil:heating:watertoairheatpump:equationfit", 2225);
        objectSeq.put("coil:heating:watertoairheatpump:variablespeedequationfit", 2226);
        objectSeq.put("coil:waterheating:airtowaterheatpump", 2227);
        objectSeq.put("coil:waterheating:desuperheater", 2228);
        objectSeq.put("coilsystem:cooling:dx", 2229);
        objectSeq.put("coilsystem:heating:dx", 2230);
        objectSeq.put("coilsystem:cooling:water:heatexchangerassisted", 2231);
        objectSeq.put("coilsystem:cooling:dx:heatexchangerassisted", 2232);
        
        objectSeq.put("evaporativecooler:direct:celdekpad", 2301);
        objectSeq.put("evaporativecooler:indirect:celdekpad", 2302);
        objectSeq.put("evaporativecooler:indirect:wetcoil", 2303);
        objectSeq.put("evaporativecooler:indirect:researchspecial", 2304);
        objectSeq.put("evaporativecooler:direct:researchspecial", 2305);
        
        objectSeq.put("humidifier:steam:electric", 2401);
        objectSeq.put("dehumidifier:desiccant:nofans", 2403);
        objectSeq.put("dehumidifier:desiccant:system", 2404);
        
        objectSeq.put("heatexchanger:airtoair:flatplate", 2501);
        objectSeq.put("heatexchanger:airtoair:sensibleandlatent", 2502);
        objectSeq.put("heatexchanger:desiccant:balancedflow", 2503);
        objectSeq.put("heatexchanger:desiccant:balancedflow:performancedatatype1", 2504);

        objectSeq.put("airloophvac:unitary:furnace:heatonly", 2601);
        objectSeq.put("airloophvac:unitary:furnace:heatcool", 2602);
        objectSeq.put("airloophvac:unitaryheatonly", 2603);
        objectSeq.put("airloophvac:unitaryheatcool", 2604);
        objectSeq.put("airloophvac:unitaryheatpump:airtoair", 2605);
        objectSeq.put("airloophvac:unitaryheatpump:watertoair", 2606);
        objectSeq.put("airloophvac:unitaryheatcool:vavchangeoverbypass", 2607);
        objectSeq.put("airloophvac:unitaryheatpump:airtoair:multispeed", 2608);
        
        objectSeq.put("airconditioner:variablerefrigerantflow", 2701);
        objectSeq.put("zoneterminalunitlist", 2702);
        
        objectSeq.put("controller:watercoil", 2801);
        objectSeq.put("controller:outdoorair", 2802);
        objectSeq.put("controller:mechanicalventilation", 2803);
        objectSeq.put("airloophvac:controllerlist", 2804);
        
        objectSeq.put("airloophvac", 2901);
        objectSeq.put("airloophvac:outdoorairsystem:equipmentlist", 2902);
        objectSeq.put("airloophvac:outdoorairsystem", 2903);
        objectSeq.put("outdoorair:mixer", 2904);
        objectSeq.put("airloophvac:zonesplitter", 2905);
        objectSeq.put("airloophvac:supplyplenum", 2906);
        objectSeq.put("airloophvac:supplypath", 2907);
        objectSeq.put("airloophvac:zonemixer", 2908);
        objectSeq.put("airloophvac:returnplenum", 2909);
        objectSeq.put("airloophvac:returnpath", 2910);
        
        objectSeq.put("branch", 3001);
        objectSeq.put("branchlist", 3002);
        objectSeq.put("connector:splitter", 3003);
        objectSeq.put("connector:mixer", 3004);
        objectSeq.put("connectorlist", 3005);
        objectSeq.put("nodelist", 3006);
        objectSeq.put("outdoorair:node", 3007);
        objectSeq.put("outdoorair:nodelist", 3008);
        objectSeq.put("pipe:adiabatic", 3009);
        objectSeq.put("pipe:adiabatic:steam", 3010);
        objectSeq.put("pipe:indoor", 3011);
        objectSeq.put("pipe:outdoor", 3012);
        objectSeq.put("pipe:underground", 3013);
        objectSeq.put("pipingsystem:underground:domain", 3014);
        objectSeq.put("pipingsystem:underground:pipecircuit", 3015);
        objectSeq.put("pipingsystem:underground:pipesegment", 3016);
        objectSeq.put("duct", 3017);
        
        objectSeq.put("pump:variablespeed", 3101);
        objectSeq.put("pump:constantspeed", 3102);
        objectSeq.put("pump:variablespeed:condensate", 3103);
        objectSeq.put("headeredpumps:constantspeed", 3104);
        objectSeq.put("headeredpumps:variablespeed", 3105);
        
        objectSeq.put("temperingvalve", 3201);
        
        objectSeq.put("loadprofile:plant", 3301);
        
        objectSeq.put("solarcollectorperformance:flatplate", 3401);
        objectSeq.put("solarcollector:flatplate:water", 3402);
        objectSeq.put("solarcollector:flatplate:photovoltaicthermal", 3403);
        objectSeq.put("solarcollectorperformance:photovoltaicthermal:simple", 3404);
        objectSeq.put("solarcollector:integralcollectorstorage", 3405);
        objectSeq.put("solarcollectorperformance:integralcollectorstorage", 3406);
        objectSeq.put("solarcollector:unglazedtranspired", 3407);
        objectSeq.put("solarcollector:unglazedtranspired:multisystem", 3408);
        
        objectSeq.put("boiler:hotwater", 3501);
        objectSeq.put("boiler:steam", 3502);
        objectSeq.put("chiller:electric:eir", 3503);
        objectSeq.put("chiller:electric:reformulatedeir", 3504);
        objectSeq.put("chiller:electric", 3505);
        objectSeq.put("chiller:absorption:indirect", 3506);
        objectSeq.put("chiller:absorption", 3507);
        objectSeq.put("chiller:constantcop", 3508);
        objectSeq.put("chiller:enginedriven", 3509);
        objectSeq.put("chiller:combustionturbine", 3510);
        objectSeq.put("chillerheater:absorption:directfired", 3511);
        objectSeq.put("chillerheater:absorption:doubleeffect", 3512);
        objectSeq.put("heatpump:watertowater:equationfit:heating", 3513);
        objectSeq.put("heatpump:watertowater:equationfit:cooling", 3514);
        objectSeq.put("heatpump:watertowater:parameterestimation:cooling", 3515);
        objectSeq.put("heatpump:watertowater:parameterestimation:heating", 3516);
        objectSeq.put("districtcooling", 3517);
        objectSeq.put("districtheating", 3518);
        objectSeq.put("plantcomponent:temperaturesource", 3519);
        objectSeq.put("centralheatpumpsystem", 3520);
        objectSeq.put("chillerheaterperformance:electric:eir", 3521);
        
        objectSeq.put("coolingtower:singlespeed", 3601);
        objectSeq.put("coolingtower:twospeed", 3602);
        objectSeq.put("coolingtower:variablespeed", 3603);
        objectSeq.put("coolingtowerperformance:cooltools", 3604);
        objectSeq.put("coolingtowerperformance:yorkcalc", 3605);
        objectSeq.put("evaporativefluidcooler:singlespeed", 3606);
        objectSeq.put("evaporativefluidcooler:twospeed", 3607);
        objectSeq.put("fluidcooler:singlespeed", 3608);
        objectSeq.put("fluidcooler:twospeed", 3609);
        objectSeq.put("groundheatexchanger:vertical", 3610);
        objectSeq.put("groundheatexchanger:pond", 3611);
        objectSeq.put("groundheatexchanger:surface", 3612);
        objectSeq.put("groundheatexchanger:horizontaltrench", 3613);
        objectSeq.put("heatexchanger:fluidtofluid", 3614);
        
        objectSeq.put("waterheater:mixed", 3701);
        objectSeq.put("waterheater:stratified", 3702);
        objectSeq.put("waterheater:sizing", 3703);
        objectSeq.put("waterheater:heatpump", 3704);
        objectSeq.put("thermalstorage:ice:simple", 3705);
        objectSeq.put("thermalstorage:ice:detailed", 3706);
        objectSeq.put("thermalstorage:chilledwater:mixed", 3707);
        objectSeq.put("thermalstorage:chilledwater:stratified", 3708);
        
        objectSeq.put("plantloop", 3801);
        objectSeq.put("condenserloop", 3802);
        
        objectSeq.put("plantequipmentlist", 3901);
        objectSeq.put("condenserequipmentlist", 3902);
        objectSeq.put("plantequipmentoperation:uncontrolled", 3903);
        objectSeq.put("plantequipmentoperation:coolingload", 3904);
        objectSeq.put("plantequipmentoperation:heatingload", 3905);
        objectSeq.put("plantequipmentoperation:outdoordrybulb", 3906);
        objectSeq.put("plantequipmentoperation:outdoorwetbulb", 3907);
        objectSeq.put("plantequipmentoperation:outdoorrelativehumidity", 3908);
        objectSeq.put("plantequipmentoperation:outdoordewpoint", 3909);
        objectSeq.put("plantequipmentoperation:componentsetpoint", 3910);
        objectSeq.put("plantequipmentoperation:outdoordrybulbdifference", 3911);
        objectSeq.put("plantequipmentoperation:outdoorwetbulbdifference", 3912);
        objectSeq.put("plantequipmentoperation:outdoordewpointdifference", 3913);
        objectSeq.put("plantequipmentoperationschemes", 3914);
        objectSeq.put("condenserequipmentoperationschemes", 3915);
        
        objectSeq.put("energymanagementsystem:sensor", 4001);
        objectSeq.put("energymanagementsystem:actuator", 4002);
        objectSeq.put("energymanagementsystem:programcallingmanager", 4003);
        objectSeq.put("energymanagementsystem:program", 4004);
        objectSeq.put("energymanagementsystem:subroutine", 4005);
        objectSeq.put("energymanagementsystem:globalvariable", 4006);
        objectSeq.put("energymanagementsystem:outputvariable", 4007);
        objectSeq.put("energymanagementsystem:meteredoutputvariable", 4008);
        objectSeq.put("energymanagementsystem:trendvariable", 4009);
        objectSeq.put("energymanagementsystem:internalvariable", 4010);
        objectSeq.put("energymanagementsystem:curveortableindexvariable", 4011);
        objectSeq.put("energymanagementsystem:constructionindexvariable", 4012);
        
        objectSeq.put("externalinterface", 4101);
        objectSeq.put("externalinterface:schedule", 4102);
        objectSeq.put("externalinterface:variable", 4103);
        objectSeq.put("externalinterface:actuator", 4104);
        objectSeq.put("externalinterface:functionalmockupunitimport", 4105);
        objectSeq.put("externalinterface:functionalmockupunitimport:from:variable", 4106);
        objectSeq.put("externalinterface:functionalmockupunitimport:to:schedule", 4107);
        objectSeq.put("externalinterface:functionalmockupunitimport:to:actuator", 4108);
        objectSeq.put("externalinterface:functionalmockupunitimport:to:variable", 4109);
        objectSeq.put("externalinterface:functionalmockupunitexport:from:variable", 4110);
        objectSeq.put("externalinterface:functionalmockupunitexport:to:schedule", 4111);
        objectSeq.put("externalinterface:functionalmockupunitexport:to:actuator", 4112);
        objectSeq.put("externalinterface:functionalmockupunitexport:to:variable", 4113);
        
        objectSeq.put("zonehvac:forcedair:userdefined", 4201);
        objectSeq.put("airterminal:singleduct:userdefined", 4202);
        objectSeq.put("coil:userdefined", 4203);
        objectSeq.put("plantcomponent:userdefined", 4204);
        
        objectSeq.put("availabilitymanager:scheduled", 4301);
        objectSeq.put("availabilitymanager:scheduledon", 4302);
        objectSeq.put("availabilitymanager:scheduledoff", 4303);
        objectSeq.put("availabilitymanager:nightcycle", 4304);
        objectSeq.put("availabilitymanager:differentialthermostat", 4305);
        objectSeq.put("availabilitymanager:hightemperatureturnoff", 4306);
        objectSeq.put("availabilitymanager:hightemperatureturnon", 4307);
        objectSeq.put("availabilitymanager:lowtemperatureturnoff", 4308);
        objectSeq.put("availabilitymanager:lowtemperatureturnon", 4309);
        objectSeq.put("availabilitymanager:nightventilation", 4310);
        objectSeq.put("availabilitymanager:hybridventilation", 4311);
        objectSeq.put("availabilitymanagerassignmentlist", 4312);
        
        objectSeq.put("setpointmanager:scheduled", 4401);
        objectSeq.put("setpointmanager:scheduled:dualsetpoint", 4402);
        objectSeq.put("setpointmanager:outdoorairreset", 4403);
        objectSeq.put("setpointmanager:singlezone:reheat", 4404);
        objectSeq.put("setpointmanager:singlezone:heating", 4405);
        objectSeq.put("setpointmanager:singlezone:cooling", 4406);
        objectSeq.put("setpointmanager:singlezone:humidity:minimum", 4407);
        objectSeq.put("setpointmanager:singlezone:humidity:maximum", 4408);
        objectSeq.put("setpointmanager:mixedair", 4409);
        objectSeq.put("setpointmanager:outdoorairpretreat", 4410);
        objectSeq.put("setpointmanager:warmest", 4411);
        objectSeq.put("setpointmanager:coldest", 4412);
        objectSeq.put("setpointmanager:returnairbypassflow", 4413);
        objectSeq.put("setpointmanager:warmesttemperatureflow", 4414);
        objectSeq.put("setpointmanager:multizone:heating:average", 4415);
        objectSeq.put("setpointmanager:multizone:cooling:average", 4416);
        objectSeq.put("setpointmanager:multizone:minimumhumidity:average", 4417);
        objectSeq.put("setpointmanager:multizone:maximumhumidity:average", 4418);
        objectSeq.put("setpointmanager:multizone:humidity:minimum", 4419);
        objectSeq.put("setpointmanager:multizone:humidity:maximum", 4420);
        objectSeq.put("setpointmanager:followoutdoorairtemperature", 4421);
        objectSeq.put("setpointmanager:followsystemnodetemperature", 4422);
        objectSeq.put("setpointmanager:followgroundtemperature", 4423);
        objectSeq.put("setpointmanager:condenserenteringreset", 4424);
        objectSeq.put("setpointmanager:condenserenteringreset:ideal", 4425);
        
        objectSeq.put("refrigeration:case", 4501);
        objectSeq.put("refrigeration:compressorrack", 4502);
        objectSeq.put("refrigeration:caseandwalkinlist", 4503);
        objectSeq.put("refrigeration:condenser:aircooled", 4504);
        objectSeq.put("refrigeration:condenser:evaporativecooled", 4505);
        objectSeq.put("refrigeration:condenser:watercooled", 4506);
        objectSeq.put("refrigeration:condenser:cascade", 4507);
        objectSeq.put("refrigeration:gascooler:aircooled", 4508);
        objectSeq.put("refrigeration:transferloadlist", 4509);
        objectSeq.put("refrigeration:subcooler", 4510);
        objectSeq.put("refrigeration:compressor", 4511);
        objectSeq.put("refrigeration:compressorlist", 4512);
        objectSeq.put("refrigeration:system", 4513);
        objectSeq.put("refrigeration:transcriticalsystem", 4514);
        objectSeq.put("refrigeration:secondarysystem", 4515);
        objectSeq.put("refrigeration:walkin", 4516);
        objectSeq.put("refrigeration:airchiller", 4517);
        objectSeq.put("zonehvac:refrigerationchillerset", 4518);
        
        objectSeq.put("demandmanagerassignmentlist", 4601);
        objectSeq.put("demandmanager:exteriorlights", 4602);
        objectSeq.put("demandmanager:lights", 4603);
        objectSeq.put("demandmanager:electricequipment", 4604);
        objectSeq.put("demandmanager:thermostats", 4605);
        
        objectSeq.put("generator:internalcombustionengine", 4701);
        objectSeq.put("generator:combustionturbine", 4702);
        objectSeq.put("generator:microturbine", 4703);
        objectSeq.put("generator:photovoltaic", 4704);
        objectSeq.put("photovoltaicperformance:simple", 4705);
        objectSeq.put("photovoltaicperformance:equivalentone-diode", 4706);
        objectSeq.put("photovoltaicperformance:sandia", 4707);
        objectSeq.put("generator:fuelcell", 4708);
        objectSeq.put("generator:fuelcell:powermodule", 4709);
        objectSeq.put("generator:fuelcell:airsupply", 4710);
        objectSeq.put("generator:fuelcell:watersupply", 4711);
        objectSeq.put("generator:fuelcell:auxiliaryheater", 4712);
        objectSeq.put("generator:fuelcell:exhaustgastowaterheatexchanger", 4713);
        objectSeq.put("generator:fuelcell:electricalstorage", 4714);
        objectSeq.put("generator:fuelcell:inverter", 4715);
        objectSeq.put("generator:fuelcell:stackcooler", 4716);
        objectSeq.put("generator:microchp", 4717);
        objectSeq.put("generator:microchp:nonnormalizedparameters", 4718);
        objectSeq.put("generator:fuelsupply", 4719);
        objectSeq.put("generator:windturbine", 4720);
        objectSeq.put("electricloadcenter:generators", 4721);
        objectSeq.put("electricloadcenter:inverter:simple", 4722);
        objectSeq.put("electricloadcenter:inverter:functionofpower", 4723);
        objectSeq.put("electricloadcenter:inverter:lookuptable", 4724);
        objectSeq.put("electricloadcenter:storage:simple", 4725);
        objectSeq.put("electricloadcenter:storage:battery", 4726);
        objectSeq.put("electricloadcenter:transformer", 4727);
        objectSeq.put("electricloadcenter:distribution", 4728);
        
        objectSeq.put("wateruse:equipment", 4801);
        objectSeq.put("wateruse:connections", 4802);
        objectSeq.put("wateruse:storage", 4803);
        objectSeq.put("wateruse:well", 4804);
        objectSeq.put("wateruse:raincollector", 4805);
        
        //TODO no 49XX
        
        objectSeq.put("matrix:twodimension", 4901);
        
        objectSeq.put("curve:linear", 5001);
        objectSeq.put("curve:quadlinear", 5002);
        objectSeq.put("curve:quadratic", 5003);
        objectSeq.put("curve:cubic", 5004);
        objectSeq.put("curve:quartic", 5005);
        objectSeq.put("curve:exponent", 5006);
        objectSeq.put("curve:bicubic", 5007);
        objectSeq.put("curve:biquadratic", 5008);
        objectSeq.put("curve:quadraticlinear", 5009);
        objectSeq.put("curve:triquadratic", 5011);
        objectSeq.put("curve:functional:pressuredrop", 5012);
        objectSeq.put("curve:fanpressurerise", 5013);
        objectSeq.put("curve:exponentialskewnormal", 5014);
        objectSeq.put("curve:sigmoid", 5015);
        objectSeq.put("curve:rectangularhyperbola1", 5016);
        objectSeq.put("curve:rectangularhyperbola2", 5017);
        objectSeq.put("curve:exponentialdecay", 5018);
        objectSeq.put("curve:doubleexponentialdecay", 5019);
        
        objectSeq.put("table:oneindependentvariable", 5101);
        objectSeq.put("table:twoindependentvariables", 5102);
        objectSeq.put("table:multivariablelookup", 5103);
        
        objectSeq.put("fluidproperties:name", 5201);
        objectSeq.put("fluidproperties:glycolconcentration", 5202);
        objectSeq.put("fluidproperties:temperatures", 5203);
        objectSeq.put("fluidproperties:saturated", 5204);
        objectSeq.put("fluidproperties:superheated", 5205);
        objectSeq.put("fluidproperties:concentration", 5206);
        
        objectSeq.put("currencytype", 5301);
        objectSeq.put("componentcost:adjustments", 5302);
        objectSeq.put("componentcost:reference", 5303);
        objectSeq.put("componentcost:lineitem", 5304);
        objectSeq.put("utilitycost:tariff", 5305);
        objectSeq.put("utilitycost:qualify", 5306);
        objectSeq.put("utilitycost:charge:simple", 5307);
        objectSeq.put("utilitycost:charge:block", 5308);
        objectSeq.put("utilitycost:ratchet", 5309);
        objectSeq.put("utilitycost:variable", 5310);
        objectSeq.put("utilitycost:computation", 5311);
        objectSeq.put("lifecyclecost:parameters", 5312);
        objectSeq.put("lifecyclecost:recurringcosts", 5313);
        objectSeq.put("lifecyclecost:nonrecurringcost", 5314);
        objectSeq.put("lifecyclecost:usepriceescalation", 5315);
        objectSeq.put("lifecyclecost:useadjustment", 5316);
        
        objectSeq.put("parametric:setvalueforrun", 5401);
        objectSeq.put("parametric:logic", 5402);
        objectSeq.put("parametric:runcontrol", 5403);
        objectSeq.put("parametric:filenamesuffix", 5404);
        
        objectSeq.put("output:variabledictionary", 5501);
        objectSeq.put("output:surfaces:list", 5502);
        objectSeq.put("output:surfaces:drawing", 5503);
        objectSeq.put("output:schedules", 5504);
        objectSeq.put("output:constructions", 5505);
        objectSeq.put("output:energymanagementsystem", 5506);
        objectSeq.put("outputcontrol:surfacecolorscheme", 5507);
        objectSeq.put("output:table:summaryreports", 5508);
        objectSeq.put("output:table:timebins", 5509);
        objectSeq.put("output:table:monthly", 5510);
        objectSeq.put("outputcontrol:table:style", 5511);
        objectSeq.put("outputcontrol:reportingtolerances", 5512);
        objectSeq.put("output:variable", 5513);
        objectSeq.put("output:meter", 5514);
        objectSeq.put("output:meter:meterfileonly", 5515);
        objectSeq.put("output:meter:cumulative", 5516);
        objectSeq.put("output:meter:cumulative:meterfileonly", 5517);
        objectSeq.put("meter:custom", 5518);
        objectSeq.put("meter:customdecrement", 5519);
        objectSeq.put("output:sqlite", 5520);
        objectSeq.put("output:environmentalimpactfactors", 5521);
        objectSeq.put("environmentalimpactfactors", 5522);
        objectSeq.put("fuelfactors", 5523);
        objectSeq.put("output:diagnostics", 5524);
        objectSeq.put("output:debuggingdata", 5525);
        objectSeq.put("output:preprocessormessage", 5526);
    }

    private static void initCategorySeq(){
        categorySeq = new HashMap<>();
        
        categorySeq.put(0, "Simulation Parameters");
        categorySeq.put(1, "Compliance Objects");
        categorySeq.put(2, "Location and Climate");
        categorySeq.put(3, "Schedules");
        categorySeq.put(4, "Surface Construction Elements");
        categorySeq.put(5, "Thermal Zones and Surfaces");
        categorySeq.put(6, "Advanced Construction, Surface, Zone Concepts");
        categorySeq.put(7, "Detailed Ground Heat Transfer");
        categorySeq.put(8, "Room Air Models");
        categorySeq.put(9, "Internal Gains");
        categorySeq.put(10, "Daylighting");
        categorySeq.put(11, "Zone Airflow");
        categorySeq.put(12, "Natural Ventilation and Duct Leakage");
        categorySeq.put(13, "Exterior Equipment");
        categorySeq.put(14, "HVAC Templates");
        categorySeq.put(15, "HVAC Design Objects");
        categorySeq.put(16, "Zone HVAC Controls and Thermostats");
        categorySeq.put(17, "Zone HVAC Forced Air Units");
        categorySeq.put(18, "Zone HVAC Radiative/Convective Units");
        categorySeq.put(19, "Zone HVAC Air Loop Terminal Units");
        categorySeq.put(20, "Zone HVAC Equipment Connections");
        categorySeq.put(21, "Fans");
        categorySeq.put(22, "Coils");
        categorySeq.put(23, "Evaporative Coolers");
        categorySeq.put(24, "Humidifiers and Dehumidifiers");
        categorySeq.put(25, "Heat Recovery");
        categorySeq.put(26, "Unitary Equipment");
        categorySeq.put(27, "Variable Refrigerant Flow Equipment");
        categorySeq.put(28, "Controllers");
        categorySeq.put(29, "Air Distribution");
        categorySeq.put(30, "Node-Branch Management");
        categorySeq.put(31, "Pumps");
        categorySeq.put(32, "Plant-Condenser Flow Control");
        categorySeq.put(33, "Non-Zone Equipment");
        categorySeq.put(34, "Solar Collectors");
        categorySeq.put(35, "Plant Heating and Cooling Equipment");
        categorySeq.put(36, "Condenser Equipment and Heat Exchangers");
        categorySeq.put(37, "Water Heaters and Thermal Storage");
        categorySeq.put(38, "Plant-Condenser Loops");
        categorySeq.put(39, "Plant-Condenser Control");
        categorySeq.put(40, "Energy Management System (EMS)");
        categorySeq.put(41, "External Interface");
        categorySeq.put(42, "User Defined HVAC and Plant Component Models");
        categorySeq.put(43, "System Availability Managers");
        categorySeq.put(44, "Setpoint Managers");
        categorySeq.put(45, "Refrigeration");
        categorySeq.put(46, "Demand Limiting Controls");
        categorySeq.put(47, "Electric Load Center-Generator Specifications");
        categorySeq.put(48, "Water Systems");
        categorySeq.put(49, "General Data Entry");
        categorySeq.put(50, "Performance Curves");
        categorySeq.put(51, "Performance Tables");
        categorySeq.put(52, "Fluid Properties");
        categorySeq.put(53, "Economics");
        categorySeq.put(54, "Parametrics");
        categorySeq.put(55, "Output Reporting");
    }

    public int getObjectSeq(String objLabel){
        if(objectSeq == null){
            initObjectSeq();
        }
        
        if(objectSeq.containsKey(objLabel)){
            return objectSeq.get(objLabel);
        }
        
        return -1;
    }
    
    public String getCategory(int categorySeqNumber){
        if(categorySeq == null){
            initCategorySeq();
        }
        
        if(categorySeq.containsKey(categorySeqNumber)){
            return categorySeq.get(categorySeqNumber);
        }
        
        return null;
    }
}

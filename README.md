# IDFProject
 JAVA for EnergyPlus

IDFProject is a project that I have been working on since 2017. The purpose of this project is to develop an EnergyPlus-based model processor that helps business quickly build their cloud simulation infrastructure.

My previous project, BuildSim Cloud, was built on top of IDFProject, deployed in both AWS and Azure, and has since complete over 3 million simulations.

After years of testing, I think the IDFProject is robust enough for public release.


How to use?

# Installation requirements

The package can be downloaded as a Java project and run on Eclipse or other JAVA IDEs.
You will need to turn the project to a maven project.

Java 1.8+
Maven


# Process IDFs

Below is the sample code to process an IDF file
```Java
// 1. tell IDFProject where to look for the configuration file
ServerConfig.setConfigPath("/Users/[USERNAME]/eclipse-workspace/IDFProject/resources/server.config");

// 2. tell IDFProject the target IDF file directory, and save it in a Java file object
File idfFile = new File("/Users/[USERNAME]/Desktop/data/mediumoffice87.idf");

// 3. create an empty IDFFileObject
IDFFileObject model = new IDFFileObject();

//4. create an empty IDFParser
IDFParser parser = new IDFParser();

//5. process the idfFile
parser.parseIDFFromLocalMachine(idfFile, model);

```
Once the model is processed, information can then be easily extracted from the model via multiple functions such as getCategoryList(*IDF_Label_Name*):
```Java
List<IDFObject> runperiod = model.getCategoryList("RunPeriodControl:SpecialDays");
for(IDFObject idfObj in runperiod){
    System.out.println(idfObj.printStatement(100))
}
```
Output:
```
RunPeriodControl:SpecialDays,
    Christmas,                                                                                           !- Name
    December 25,                                                                                         !- Start Date
    1,                                                                                                   !- Duration {days}
    Holiday;                                                                                             !- Special Day Type

RunPeriodControl:SpecialDays,
    Columbus Day,                                                                                        !- Name
    2nd Monday in October,                                                                               !- Start Date
    1,                                                                                                   !- Duration {days}
    Holiday;                                                                                             !- Special Day Type
    ...
```

You can also search for a IDFObject

You can also add new IDFObject to the EnergyPlus file or remove an object using the model. More functions 
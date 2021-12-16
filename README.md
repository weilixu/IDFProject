# IDFProject
 JAVA for EnergyPlus

IDFProject is a project that I have been working on since 2017. The purpose of this project is to develop an EnergyPlus-based model processor that helps business quickly build their cloud simulation infrastructure.

My previous project, BuildSim Cloud, was built on top of IDFProject, deployed in both AWS and Azure, and has since complete over 3 million simulations.

After years of testing, I think the IDFProject is robust enough for public release.


How to use?

# Installation requirements

The package can be downloaded as a Java project and run on Eclipse or other JAVA IDEs.
You will need to turn the project to a maven project.

1. Download the latest JRE (if not installed) from: https://www.java.com/en/download/manual.jsp
2. Download the latest JDK from: https://www.oracle.com/java/technologies/downloads/
3. Download a Java IDE - It is recommended to use JetBrain's [IntelliJ](https://www.jetbrains.com/idea/) or [Eclipse](https://www.eclipse.org/downloads/)
4. Clone the project to your local folder (git clone...)
5. Import the project to your IDE and configure it as Maven Project. 
   1. IntelliJ should automatically detect the Maven file
   2. For Eclipse, you need to manually turn the project to Maven project and refresh the project.
6. Modify the server.config file under resources folder.
```aidl
ResourcePath= [YOUR PATH]\\IDFProject\\resources\\
ValidateSSLCertificate=No
debug=false
platform=local
ViewerDataSavedPath=[YOUR PATH]\\IDFProject\\WebContent\\temp
```
7. Open the ViewerGenerator file, and configure the paths to kick off geometry viewer.
```aidl
        ServerConfig.setConfigPath("[YOUR PATH]\\IDFProject\\resources\\server.config");
        File idfFile = new File("[IDF FILE PATH]");
```
The viewer will automatically appear in your default browser.

# Process IDFs
Below is the sample code to process an IDF file
```Java
// 1. tell IDFProject where to look for the configuration file, the server.config should be under the resources folder
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
Once the model is processed, information can be easily extracted from the model via multiple functions such as getCategoryList(*IDF_Label_Name*):
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


# Search an IDF object and modify its parameter
The example below searches for the object: Lights inside the EnergyPlus file and then modify the data in watts per zone floor area field.

```Java
List<IDFObject> lpdList = idfFileObject.getCategoryList("Lights");
        if (lpdList != null) {
            for (int i = 0; i < lpdList.size(); i++) {
                IDFObject lpdObj = lpdList.get(i);
                lpdObj.setDataByStandardComment("Design Level Calculation Method", "Watts/Area");
                lpdObj.setDataByStandardComment("Watts per Zone Floor Area", 7.8 + "");
            }
        }
```

# Remove IDF object(s)
In scenario which we need to remove an IDF object from a list of objects, we can call the ```Java IDFFileObject.removeObject("Lights")``` function to remove it.
To avoid concurrent modification exception, it is recommended to remove objects outside of a loop, for example:

```Java
List<IDFObject> lpdList = idfFileObject.getCategoryList("Lights");
List<IDFObject> toBeDeleted = new ArrayList<>();
        if (lpdList != null) {
            for (int i = 0; i < lpdList.size(); i++) {
                //check condition - short circuit to get the correct comment, and then check if any lighting object equals to 12 W/m2
                // note the getIndexedData function returns a String object
                if (lpdObj.getStandardComments()[i] == "Watts per Zone Floor Area" && lpdObj.getIndexedData(i) == "12.0"){
                    toBeDeleted.add(lpdObj);
                }
        }

// Now we can safely delete the object.
for (IDFObject del : toBeDeleted) {
    idfFileObject.removeIDFObject(del);
}

```

# Create a new IDF object
Below is a sample code for setting up an object in an IDF file.

```Java
private IDFObject generateDefaultSimpleGlazingForUValue(Double targetValue) {
        int num = 4;
        // num represents how many lines the IDF object will have. 
        IDFObject idfObject = new IDFObject("WindowMaterial:SimpleGlazingSystem", num);
        // You can insert some comments or keys in the top comment section for this object.
        idfObject.setTopComments(new String[] { "!- Generated by BuildSimHub" });

        idfObject.setIndexedStandardComment(0, "Name");
        idfObject.setIndexedData(0, "Simple Glazing BSH");

        idfObject.setIndexedStandardComment(1, "U-Factor");
        idfObject.setIndexedData(1, targetValue.toString(), "W/m2K");

        idfObject.setIndexedStandardComment(2, "Heat Gain Coefficient");
        idfObject.setIndexedData(2, "0.4", "");

        return idfObject;
    }
```

# Insert/add a new IDF object
Adding a new IDF object is straightforward as demonstrated by the example below
```Java

IDFObject simpleGlazeWindow = generateDefaultSimpleGlazingForUValue(2.2);
//add this new object to the model
idfFileObject.addObject(simpleGlazeWindow)

```

# IDF File validation
It is important to have your IDF model validated to make sure a successful simulation.
A basic validation function is provided with the package. It is currently checking the IDF file against the IDD file.
```Java
IDDParser iddParser = new IDDParser(idfFileObject.getVersion());
iddParser.validateIDF(idfFileObject);

```
The ```Java validateIDF(IDFFileObject)``` will return a message in Json format, showing the warnings and errors when checking the IDF file against IDD files.
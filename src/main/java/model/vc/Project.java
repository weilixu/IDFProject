package main.java.model.vc;

import main.java.util.StringUtil;
import main.java.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Project {
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    private int projectId;
    private String buildingType;
    private String projectName;
    private String projectDes;
    private String weatherFile;
    private String[] weatherFileSplit;

    private String country;
    private String state;
    private String address;
    private double lat;
    private double lng;
    private String zipCode;

    private String ownerId;
    private String ownerName;

    private String APIKey;

    private Integer userRightsBitMap;
    private int commitsNumber;

    private boolean isHidden = false;
    private String expireDate = null;

    public Project() {
    }

    public Project(int projectId,
                   String buildingType,
                   String projectName,
                   String projectDes,
                   String weatherFile,
                   String country,
                   String state,
                   String address,
                   double lat,
                   double lng,
                   Integer userRightsBitMap,
                   String ownerId,
                   String ownerName) {
        this.projectId = projectId;
        this.buildingType = buildingType;
        this.projectName = projectName;
        this.projectDes = projectDes;
        this.weatherFile = weatherFile;

        if (weatherFile != null) {
            this.weatherFileSplit = weatherFile.split("_");
        }

        this.country = country;
        this.state = state;
        this.address = address;
        this.lat = lat;
        this.lng = lng;

        this.userRightsBitMap = userRightsBitMap;

        this.ownerId = ownerId;
        this.ownerName = ownerName;

        this.commitsNumber = 0;
    }

    public static String makeUserAPIProjectName(String userId) {
        return "API_Project_user_" + userId;
    }

    public static String makeProjectAPIProjectName(String projectId) {
        return "API_Project_project_" + projectId;
    }

    public static String reverseProjectAPIProject(String apiProjectName) {
        String[] split = apiProjectName.split("_");
        if (split.length > 3) {
            return split[3];
        }
        return null;
    }

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public String getBuildingType() {
        return buildingType;
    }

    public void setBuildingType(String buildingType) {
        this.buildingType = buildingType;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectDes() {
        return projectDes;
    }

    public void setProjectDes(String projectDes) {
        this.projectDes = projectDes;
    }

    public Integer getUserPriviledge() {
        return userRightsBitMap;
    }

    public int getCommitsNumber() {
        return commitsNumber;
    }

    public void setCommitsNumber(int commitsNumber) {
        this.commitsNumber = commitsNumber;
    }

    public Integer getUserRightsBitMap() {
        return userRightsBitMap;
    }

    public void setUserRightsBitMap(Integer userRightsBitMap) {
        this.userRightsBitMap = userRightsBitMap;
    }

    public String getWeatherFile() {
        return weatherFile;
    }

    public void setWeatherFile(String weatherFile) {
        this.weatherFile = weatherFile;
        this.weatherFileSplit = weatherFile.split("_");
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getWeatherFileCountry() {
        if (this.weatherFileSplit != null && this.weatherFileSplit.length > 1) {
            return weatherFileSplit[0];
        }
        return null;
    }

    public String getWeatherFileState() {
        if (this.weatherFileSplit != null && this.weatherFileSplit.length > 1) {
            return weatherFileSplit[1];
        }
        return null;
    }

    public String getAPIKey() {
        return APIKey;
    }

    public void setAPIKey(String APIKey) {
        this.APIKey = APIKey;
    }

    public boolean isHidden() {
        return this.isHidden;
    }

    public void setHidden(boolean hidden) {
        this.isHidden = hidden;
    }

    public String getExpireDate() {
        return expireDate;
    }

    public void setExpireDate(String expireDate) {
        this.expireDate = StringUtil.checkNullAndEmpty(expireDate, "N/A");
    }

    public boolean isExpired() {
        /*if(getProjectTier()==ProjectTier.ENTERPRISE){
            Date enterpriseExpires = UserControlDAOTestImpl.getInstance().getEnterpriseUserExpireDate(ownerId);
            if(enterpriseExpires!=null){
                String expire = TimeUtil.getDate(enterpriseExpires);
                if(!expire.equals("expireDate")){
                    expireDate = expire;

                    UserControlDAOTestImpl.getInstance().setProjectExpiration(projectId+"", expireDate);
                }
            }
        }*/

        boolean expired = false;
        if (!StringUtil.isNullOrEmpty(expireDate) && !"N/A".equalsIgnoreCase(expireDate)) {
            expired = TimeUtil.isPassed(expireDate);
        }

        LOG.info("Project expire date: " + expireDate + ", expired? " + expired);
        return expired;
    }
}

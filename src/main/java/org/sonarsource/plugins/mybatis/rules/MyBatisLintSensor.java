package org.sonarsource.plugins.mybatis.rules;

import static org.sonarsource.plugins.mybatis.MyBatisPlugin.SONAR_MYBATIS_SKIP;
import static org.sonarsource.plugins.mybatis.MyBatisPlugin.STMTID_EXCLUDE_KEY;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentType;
import org.dom4j.Element;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.config.Configuration;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.xml.Xml;
import org.sonarsource.plugins.mybatis.Constant;
import org.sonarsource.plugins.mybatis.utils.IOUtils;
import org.sonarsource.plugins.mybatis.xml.MyBatisMapperXmlHandler;
import org.sonarsource.plugins.mybatis.xml.XmlParser;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/**
 * The goal of this Sensor is analysis mybatis mapper files and generate issues.
 */
public class MyBatisLintSensor implements Sensor {

    private static final Logger LOGGER = Loggers.get(MyBatisLintSensor.class);

    private static final String LEFT_SLASH = "/";
	private boolean insideStatement = false;
	private String ruleHolder = "";
	private boolean whereExistance = false;
	private boolean setTraverse = false;
	private File file_to_scan;
	private String xmlFilePath = "";

    protected final Configuration config;
    protected final FileSystem fileSystem;
    protected SensorContext context;
    private List<String> stmtIdExcludeList = new ArrayList<>();

    /**
     * Use of IoC to get Settings, FileSystem, RuleFinder and ResourcePerspectives
     */
    public MyBatisLintSensor(final Configuration config, final FileSystem fileSystem) {
        this.config = config;
        this.fileSystem = fileSystem;
    }

    @Override
    public void describe(final SensorDescriptor descriptor) {
        descriptor.name("MyBatisLint Sensor");
        descriptor.onlyOnLanguage(Xml.KEY);
    }

    @Override
    public void execute(final SensorContext context) {
        this.context = context;
        Boolean sonarMyBatisSkipBooleanValue = Boolean.valueOf(false);
        Optional<Boolean> sonarMyBatisSkipValue = config.getBoolean(SONAR_MYBATIS_SKIP);
        if (sonarMyBatisSkipValue.isPresent()) {
            sonarMyBatisSkipBooleanValue = sonarMyBatisSkipValue.get();
        }
        if (Boolean.TRUE.equals(sonarMyBatisSkipBooleanValue)) {
            LOGGER.info("MyBatis sensor is skiped.");
            return;
        }
        String[] stmtIdExclude = config.getStringArray(STMTID_EXCLUDE_KEY);
        Collections.addAll(stmtIdExcludeList, stmtIdExclude);
        LOGGER.info("stmtIdExcludeList: " + stmtIdExcludeList.toString());
        // analysis mybatis mapper files and generate issues
        Map mybatisMapperMap = new HashMap(16);
        List<File> reducedFileList = new ArrayList<>();

        org.apache.ibatis.session.Configuration mybatisConfiguration = new org.apache.ibatis.session.Configuration();

        // handle mybatis mapper file and add it to mybatisConfiguration
        FileSystem fs = context.fileSystem();
        Iterable<InputFile> xmlInputFiles = fs.inputFiles(fs.predicates().hasLanguage("xml"));
        for (InputFile xmlInputFile : xmlInputFiles) {
            xmlFilePath = xmlInputFile.uri().getPath();
            File xmlFile = new File(xmlFilePath);
        	file_to_scan = xmlFile;
            try {
                XmlParser xmlParser = new XmlParser();
                Document document = xmlParser.parse(xmlFile);
                Element rootElement = document.getRootElement();
                String publicIdOfDocType = "";
                DocumentType documentType = document.getDocType();
                if (null != documentType) {
                    publicIdOfDocType = documentType.getPublicID();
                    if (null == publicIdOfDocType) {
                        publicIdOfDocType = "";
                    }
                }
                if ("mapper".equals(rootElement.getName()) && publicIdOfDocType.contains("mybatis.org")) {
                    LOGGER.info("handle mybatis mapper xml:" + xmlFilePath);
                    // handle mybatis mapper file
                    String reducedXmlFilePath = xmlFilePath + "-reduced.xml";
                    File reducedXmlFile = new File(reducedXmlFilePath);
                    reducedFileList.add(reducedXmlFile);
                    MyBatisMapperXmlHandler myBatisMapperXmlHandler = new MyBatisMapperXmlHandler();
                    myBatisMapperXmlHandler.handleMapperFile(xmlFile, reducedXmlFile);
                    mybatisMapperMap.put(reducedXmlFilePath, xmlFilePath);
                    // xmlMapperBuilder parse mapper resource
                    Resource mapperResource = new FileSystemResource(reducedXmlFile);
                    XMLMapperBuilder xmlMapperBuilder = new XMLMapperBuilder(mapperResource.getInputStream(),
                        mybatisConfiguration, mapperResource.toString(), mybatisConfiguration.getSqlFragments());
                    xmlMapperBuilder.parse();
                }
            } catch (DocumentException | IOException e) {
                LOGGER.warn(e.toString());
            }
            
        	LOGGER.info("files to be looped: " + xmlInputFile);

        }

        matchRuleAndSaveIssue(xmlFilePath);

        // clean reduced.xml
        cleanFiles(reducedFileList);
    }

    private void cleanFiles(List<File> files) {
        for (File file : files) {
            if (file.exists() && file.isFile()) {
                try {
                    Files.delete(Paths.get(new URI("file:///" + file.getAbsolutePath().replace("\\", LEFT_SLASH))));
                } catch (IOException | URISyntaxException e) {
                    LOGGER.warn(e.toString());
                }
            }
        }
    }

    private void matchRuleAndSaveIssue(String sourceMapperFilePath) {
       	
        List<ErrorDataFromLinter> violations = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file_to_scan))) {
            String line;
            int currlineNumber = 0;
            ArrayList<String> setDeclaration = new ArrayList<String>(); 

            while ((line = reader.readLine()) != null) {
                currlineNumber++;
                line.toUpperCase();
                line.toLowerCase();
                System.out.println("Analysing line: " + line);

                if (line.contains("<select")) {
                	insideStatement(true, "select");
                	System.err.println("<select> found");
                	
                }      
                
                if (line.contains("<update")) {
                	insideStatement(true, "update");
                	System.err.println("<update> found");
                }    
                
                if (line.contains("<delete")) {
                	insideStatement(true, "delete");
                	System.err.println("<delete> found");
                }    
                
                if ((line.contains("</select")) || (line.contains("</update")) || (line.contains("</delete"))) {
                	
                	//refers to the checking of where statement
                	if (whereExistance == false) {
                        if (ruleHolder == "delete") {            
                        	// Where condition not found in delete statement
                            violations.add(new ErrorDataFromLinter(Constant.MYBATIS_MAPPER_CHECK_RULE_06,
                                    "where condition not found in delete statement", sourceMapperFilePath, currlineNumber));
                            LOGGER.warn("ruleId=" + "MYBATIS_MAPPER_CHECK_RULE_06" + " errorMessage=" + "where condition not found in delete statement" + 
                                    " filePath=" + sourceMapperFilePath + " line=" + currlineNumber);
                        }
                        
                        if (ruleHolder == "update") {
                            // Where condition not found in update statement
                            violations.add(new ErrorDataFromLinter(Constant.MYBATIS_MAPPER_CHECK_RULE_05,
                                    "where condition not found in update statement", sourceMapperFilePath, currlineNumber));
                            LOGGER.warn("ruleId=" + "MYBATIS_MAPPER_CHECK_RULE_05" + " errorMessage=" + "where condition not found in update statement" + 
                                    " filePath=" + sourceMapperFilePath + " line=" + currlineNumber);
                        }
                        if (ruleHolder == "select") {	
                        	// Where condition not found in select statement
                            violations.add(new ErrorDataFromLinter(Constant.MYBATIS_MAPPER_CHECK_RULE_04,
                                    "where condition not found in select statement", sourceMapperFilePath, currlineNumber));
                            LOGGER.warn("ruleId=" + "MYBATIS_MAPPER_CHECK_RULE_04" + " errorMessage=" + "where condition not found in select statement" + 
                                    " filePath=" + sourceMapperFilePath + " line=" + currlineNumber);
                        }

                	LOGGER.warn("<" + ruleHolder + "> ended");
                	insideStatement(false, "");
                	}
                	whereExistance = false;
                }          

                //if 1=1 exists
                if (insideStatement == true && (line.matches(".*(1\\s{0,}=\\s{0,}1).*"))) {
                	
                    if (ruleHolder == "delete") {
                        violations.add(new ErrorDataFromLinter(Constant.MYBATIS_MAPPER_CHECK_RULE_03, 
                        		"delete statement should not include 1=1", sourceMapperFilePath, currlineNumber));
                    	LOGGER.warn("ruleId=" + "MYBATIS_MAPPER_CHECK_RULE_03" + " errorMessage=" + "delete statement should not include 1=1" + 
                        		" filePath=" + sourceMapperFilePath + " line=" + currlineNumber);
                    }
                    if (ruleHolder == "update") {
                        violations.add(new ErrorDataFromLinter(Constant.MYBATIS_MAPPER_CHECK_RULE_02,
                                "update statement should not include 1=1", sourceMapperFilePath, currlineNumber));
                    	LOGGER.warn("ruleId=" + "MYBATIS_MAPPER_CHECK_RULE_02" + " errorMessage=" + "update statement should not include 1=1" + 
                    			" filePath=" + sourceMapperFilePath + " line=" + currlineNumber);
                    }
                    if (ruleHolder == "select") {            
                        violations.add(new ErrorDataFromLinter(Constant.MYBATIS_MAPPER_CHECK_RULE_01,
                                "select statement should not include 1=1", sourceMapperFilePath, currlineNumber));
                    	LOGGER.warn("ruleId=" + "MYBATIS_MAPPER_CHECK_RULE_01" + " errorMessage=" + "select statement should not include 1=1" + 
                                " filePath=" + sourceMapperFilePath + " line=" + currlineNumber);
                    }
                }
                
                //if where exist
                if (insideStatement == true && line.contains("where")) {
                	whereExistance = true;
                	LOGGER.warn("where located at " + " filePath=" + sourceMapperFilePath + " line=" + currlineNumber);
                }

                if (ruleHolder == "select" && (line.contains("*"))) {
                    violations.add(new ErrorDataFromLinter(Constant.MYBATIS_MAPPER_CHECK_RULE_07,
                            "select statement should not include *", sourceMapperFilePath, currlineNumber));
                    LOGGER.warn("ruleId=" + "MYBATIS_MAPPER_CHECK_RULE_07" + " errorMessage=" + "select statement should not include *" + 
                            " filePath=" + sourceMapperFilePath + " line=" + currlineNumber);
                }
                

                if (line.contains("<set>")) {
                	setTraverse = true;
                	LOGGER.warn("<set> found");
                }  
                
                if (setTraverse == true && line.contains("=") && !line.contains("<")) {
                	int indexOfEquals = line.indexOf("=");
                	
                    if (indexOfEquals > -1) {
                        String variableName = line.substring(0, indexOfEquals);
                        String cleanedVariableName = variableName.replaceAll("\\s", "");
                        if (setDeclaration.contains(cleanedVariableName)) {
                            violations.add(new ErrorDataFromLinter(Constant.MYBATIS_MAPPER_CHECK_RULE_08,
                                    "a set statement should not declare value to a variable multiple times", sourceMapperFilePath, currlineNumber));
                        	LOGGER.warn("Variable Name: " + cleanedVariableName + " is duplicated, error!");
                        } else {
                        	setDeclaration.add(cleanedVariableName);
                        	LOGGER.info("Variable Name: " + cleanedVariableName + " is added into the list!");
                        }
                    }
                }
                
        	    if (line.contains("</set>")) {
        	    	setTraverse = false;
        	    	LOGGER.warn("<set> ended");
        	    }  
                

            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }        
        
        System.out.println("Finished scanning file: " + file_to_scan + " \n");
        // Save all the detected violations
        for (ErrorDataFromLinter violation : violations) {
            LOGGER.info("Rules added: " + violation);
            getResourceAndSaveIssue(violation);
        }
        
    }

    private void getResourceAndSaveIssue(final ErrorDataFromLinter error) {

        final FileSystem fs = context.fileSystem();
        final InputFile inputFile = fs.inputFile(fs.predicates().hasAbsolutePath(error.getFilePath()));

        if (inputFile != null) {
            saveIssue(inputFile, error.getLine(), error.getType(), error.getDescription());
        } else {
            LOGGER.error("Not able to find a InputFile with " + error.getFilePath());
        }
    }

    private void saveIssue(final InputFile inputFile, int line, final String externalRuleKey, final String message) {
        RuleKey ruleKey = RuleKey.of(getRepositoryKeyForLanguage(), externalRuleKey);

        NewIssue newIssue = context.newIssue().forRule(ruleKey).gap(2.0);

        NewIssueLocation primaryLocation = newIssue.newLocation().on(inputFile).message(message);
        if (line > 0) {
            primaryLocation.at(inputFile.selectLine(line));
        }
        newIssue.at(primaryLocation);
        LOGGER.info("Save issue: " + externalRuleKey, "; Line: " + line);
        newIssue.save();
    }

    private static String getRepositoryKeyForLanguage() {
        return MyBatisLintRulesDefinition.REPO_KEY;
    }

    @Override
    public String toString() {
        return "MyBatisLintSensor";
    }
    
    private void insideStatement(boolean inside, String state) {
    	insideStatement = inside;
    	ruleHolder = state;
    }
    

}

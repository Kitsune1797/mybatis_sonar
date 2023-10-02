package org.sonarsource.plugins.mybatis.rules;


import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.plugins.xml.Xml;
import org.sonarsource.plugins.mybatis.Constant;

public final class MyBatisLintRulesDefinition implements RulesDefinition {

    public static final String REPO_KEY = "MyBatisLint";
    protected static final String REPO_NAME = REPO_KEY;

    public static final RuleKey MYBATIS_MAPPER_CHECK_RULE_01 = RuleKey.of(REPO_KEY, Constant.MYBATIS_MAPPER_CHECK_RULE_01);
    public static final RuleKey MYBATIS_MAPPER_CHECK_RULE_02 = RuleKey.of(REPO_KEY, Constant.MYBATIS_MAPPER_CHECK_RULE_02);
    public static final RuleKey MYBATIS_MAPPER_CHECK_RULE_03 = RuleKey.of(REPO_KEY, Constant.MYBATIS_MAPPER_CHECK_RULE_03);
    public static final RuleKey MYBATIS_MAPPER_CHECK_RULE_04 = RuleKey.of(REPO_KEY, Constant.MYBATIS_MAPPER_CHECK_RULE_04);
    public static final RuleKey MYBATIS_MAPPER_CHECK_RULE_05 = RuleKey.of(REPO_KEY, Constant.MYBATIS_MAPPER_CHECK_RULE_05);
    public static final RuleKey MYBATIS_MAPPER_CHECK_RULE_06 = RuleKey.of(REPO_KEY, Constant.MYBATIS_MAPPER_CHECK_RULE_06);
    public static final RuleKey MYBATIS_MAPPER_CHECK_RULE_07 = RuleKey.of(REPO_KEY, Constant.MYBATIS_MAPPER_CHECK_RULE_07);
    public static final RuleKey MYBATIS_MAPPER_CHECK_RULE_08 = RuleKey.of(REPO_KEY, Constant.MYBATIS_MAPPER_CHECK_RULE_08);


    @Override
    public void define(Context context) {
        NewRepository repository = context.createRepository(REPO_KEY, Xml.KEY).setName(REPO_NAME);

        NewRule mybatisRule01 = repository.createRule(MYBATIS_MAPPER_CHECK_RULE_01.rule())
                .setName("select statement should not include 1=1")
                .setHtmlDescription("select statement should not include 1=1, it is a useless code and bad practice, suggest removing it.")
                .setTags("mybatis", "select")
                .setType(RuleType.BUG)
                .setSeverity(Severity.MINOR);
        mybatisRule01.setDebtRemediationFunction(mybatisRule01.debtRemediationFunctions().linear("20min"));

        NewRule mybatisRule02 = repository.createRule(MYBATIS_MAPPER_CHECK_RULE_02.rule())
                .setName("update statement should not include 1=1")
                .setHtmlDescription("update statement should not include 1=1, it is a useless code and bad practice, suggest removing it.")
                .setTags("mybatis", "update")
                .setType(RuleType.BUG)
                .setSeverity(Severity.MAJOR);
        mybatisRule02.setDebtRemediationFunction(mybatisRule02.debtRemediationFunctions().linear("20min"));

        NewRule mybatisRule03 = repository.createRule(MYBATIS_MAPPER_CHECK_RULE_03.rule())
                .setName("delete statement should not include 1=1")
                .setHtmlDescription("delete statement should not include 1=1, it is a useless code and bad practice, suggest removing it.")
                .setTags("mybatis", "delete")
                .setType(RuleType.BUG)
                .setSeverity(Severity.MAJOR);
        mybatisRule03.setDebtRemediationFunction(mybatisRule03.debtRemediationFunctions().linear("20min"));

        NewRule mybatisRule04 = repository.createRule(MYBATIS_MAPPER_CHECK_RULE_04.rule())
                .setName("where condition not found in select statement")
                .setHtmlDescription("If all parameters in the update statement of Mapper XML file are null,\n" +
                        "            the sql will not have where condition, then it will select all records from the table, which may lead to\n" +
                        "            performance issues. Suggest using simple parameters instead of elements (such as:\n" +
                        "            if,choose,when,otherwise,trim,where,set,foreach elemnt etc.) for required parameters.")
                .setTags("mybatis", "select")
                .setType(RuleType.BUG)
                .setSeverity(Severity.MINOR);
        mybatisRule04.setDebtRemediationFunction(mybatisRule04.debtRemediationFunctions().linear("20min"));

        NewRule mybatisRule05 = repository.createRule(MYBATIS_MAPPER_CHECK_RULE_05.rule())
                .setName("where condition not found in update statement")
                .setHtmlDescription("If all parameters in the update statement of Mapper XML file are null,\n" +
                        "            the sql will not have where condition, then it will update all records from the table, which will result\n" +
                        "            production accident. Suggest using simple parameters instead of elements (such as:\n" +
                        "            if,choose,when,otherwise,trim,where,set,foreach elemnt etc.) for required parameters.")
                .setTags("mybatis", "update")
                .setType(RuleType.BUG)
                .setSeverity(Severity.MAJOR);
        mybatisRule05.setDebtRemediationFunction(mybatisRule05.debtRemediationFunctions().linear("20min"));

        NewRule mybatisRule06 = repository.createRule(MYBATIS_MAPPER_CHECK_RULE_06.rule())
                .setName("where condition not found in delete statement")
                .setHtmlDescription("If all parameters in the delete statement of Mapper XML file are null,\n" +
                        "            the sql will not have where condition, then it will delete all records from the table, which will result\n" +
                        "            production accident. Suggest using simple parameters instead of elements (such as:\n" +
                        "            if,choose,when,otherwise,trim,where,set,foreach elemnt etc.) for required parameters.")
                .setTags("mybatis", "delete")
                .setType(RuleType.BUG)
                .setSeverity(Severity.CRITICAL);
        mybatisRule06.setDebtRemediationFunction(mybatisRule06.debtRemediationFunctions().linear("30min"));

        NewRule mybatisRule07 = repository.createRule(MYBATIS_MAPPER_CHECK_RULE_07.rule())
                .setName("select statement should not include *")
                .setHtmlDescription("select statement should not include *, it is not a bad practice, suggest using specific fields.")
                .setTags("mybatis", "select")
                .setType(RuleType.BUG)
                .setSeverity(Severity.MINOR);
        mybatisRule07.setDebtRemediationFunction(mybatisRule07.debtRemediationFunctions().linear("20min"));

        NewRule mybatisRule08 = repository.createRule(MYBATIS_MAPPER_CHECK_RULE_08.rule())
                .setName("a set statement should not declare value to a variable multiple times")
                .setHtmlDescription("Clarity and Maintainability: Repeating the declaration of the same variable multiple times in a <set> statement can make the SQL code less clear and harder to maintain. It can lead to redundancy and increase the chances of errors in the code, especially when updates need to be made.;  \n"
                		+ "\n"
                		+ "Performance: When an SQL statement is executed, the database engine needs to parse and process it. Declaring the same variable multiple times can potentially introduce unnecessary overhead, as the database engine may need to perform redundant calculations or validations for the same variable.;  \n"
                		+ "\n"
                		+ "Consistency: Maintaining consistency in the SQL codebase is important. When other developers review or work with the code, they may expect that a variable is updated only once within a <set> block. Deviating from this convention can lead to confusion and misinterpretation of the code's intent.;  \n"
                		+ "\n"
                		+ "Error Handling: If an error occurs during the execution of the SQL statement, it can be more challenging to troubleshoot and identify the source of the error when a variable is declared multiple times. Having a single declaration point for each variable simplifies error handling and debugging.;  \n"
                		+ "\n"
                		+ "Avoiding Unintended Behavior: Repeating variable declarations can lead to unintended behavior. For example, if the same variable is declared with different values within the same <set> block, it may not be clear which value will be used, and the behavior may not match the developer's intent.")
                .setTags("mybatis", "select")
                .setType(RuleType.BUG)
                .setSeverity(Severity.CRITICAL);
        mybatisRule08.setDebtRemediationFunction(mybatisRule08.debtRemediationFunctions().linear("20min"));

        repository.done();
    }

}

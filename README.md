Quack Import Plugin
==========

The plugin allows importing testcases to [QuAck](https://testquack.com). All modifications made on testcases in QuAck manually will remain. However, tests from the same Maven project will be reconfigured on import - new will appear, removed will disappear.


#### Import JUmit4 tests
```
<plugin>
    <groupId>com.testquack</groupId>
    <artifactId>import-maven-plugin</artifactId>
    <version>1.0</version>
    <configuration>
        <apiToken>abc</apiToken>
        <quackProject>quackui</quackProject>
        <apiEndpoint>http://quack.com/api/</apiEndpoint>
    </configuration>
    <executions>
        <execution>
            <id>quack-testcases-import</id>
            <goals>
                <goal>junit-import</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

#### Import JUmit4 tests results
```
<plugin>
    <groupId>${project.groupId}</groupId>
    <artifactId>import-maven-plugin</artifactId>
    <version>1.0</version>
    <configuration>
        <apiToken>abc</apiToken>
        <quackProject>quackui</quackProject>
        <apiEndpoint>http://quack.com/api/</apiEndpoint>
        <junitXmlResource>${project.build.directory}/surefire-reports</junitXmlResource>
    </configuration>
    <executions>
        <execution>
            <id>quack-results-import</id>
            <goals>
                <goal>junit-results-import</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

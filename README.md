Quack Import Plugin
==========

The plugin allows importing testcases to QuAck. All modifications made on testcases in QuAck manually will remain. However, tests from the same Maven project will be reconfigured on import - new will appear, removed will disappear.


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
            <phase>process-test-classes</phase>
            <goals>
                <goal>junit-import</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```
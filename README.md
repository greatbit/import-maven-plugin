Quack Import Plugin
==========

A maven plugin that allows you to import automated testcases and reports (in progress) to QuAck

#### Import JUmit4 tests
```
<plugin>
    <groupId>com.testquack</groupId>
    <artifactId>import-maven-plugin</artifactId>
    <version>1.0-SNAPSHOT</version>
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
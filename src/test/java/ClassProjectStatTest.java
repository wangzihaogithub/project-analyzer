import com.github.projectanalyzer.ClassProjectStat;
import com.github.projectanalyzer.ExportDubboMethodProjectStat;
import com.github.projectanalyzer.ExportHttpMethodProjectStat;
import com.github.projectanalyzer.ImportInterfaceClassProjectStat;
import com.github.projectanalyzer.util.Config;

import java.io.IOException;
import java.lang.instrument.IllegalClassFormatException;
import java.util.Arrays;
import java.util.List;

public class ClassProjectStatTest {
    private static final Class<?> loader = ClassProjectStatTest.class;
    private static final Config config = new Config()
            .bizPackagePath("com/ig", "com/zhipin", "com/techwolf", "cn/techwolf")
            .dubboExportAnnotations("Lcom/ig/annotation/IGService;", "Lcom/ig/annotation/BossZpService;");

    public static void main(String[] args) throws IOException, IllegalClassFormatException {
        ClassProjectStat quakeApi = new ClassProjectStat(
                loader.getResource("zhipin-quake-api-2.1.99-SNAPSHOT.jar").getFile()
        ).parse(config);

        List<ClassProjectStat> quakeHunter = buildProjectStat(
                loader.getResource("zhipin-quake-hunter.jar").getFile(),
                config, quakeApi);

        List<ClassProjectStat> quakeWeb = buildProjectStat(
                loader.getResource("zhipin-quake-web.jar").getFile(),
                config, quakeApi);

        List<ClassProjectStat> quakeStatistics = buildProjectStat(
                loader.getResource("zhipin-quake-statistics.jar").getFile(),
                config, quakeApi);

        System.out.println("end ");
    }

    private static List<ClassProjectStat> buildProjectStat(String jarPath,
                                                           Config config,
                                                           ClassProjectStat importProject) throws IllegalClassFormatException, IOException {
        List<ClassProjectStat> list = Arrays.asList(
                new ImportInterfaceClassProjectStat(jarPath, importProject),
                new ExportDubboMethodProjectStat(jarPath, importProject),
                new ExportHttpMethodProjectStat(jarPath, importProject)
        );
        for (ClassProjectStat stat : list) {
            stat.parse(config);
        }
        return list;
    }

}

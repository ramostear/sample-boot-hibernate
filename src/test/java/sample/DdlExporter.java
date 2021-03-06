package sample;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

import org.hibernate.boot.*;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.registry.*;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.springframework.boot.orm.jpa.hibernate.*;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;

/**
 * Entity 定義を元に DDL を生成します。  
 * <p>モデルファーストで開発していきたいときなどに利用して下さい。
 * <p>通常のJava実行で出力してください。出力先は OutputRoot 定数で指定されたディレクトリ配下となります。
 * <p>現状 Hibernate 5 で Bean Validation をソース元としたカラム定義がうまく出力されなくなってしまっています。( Hibernate 4 + Configuration だとうまくいっていた)<br>
 * http://stackoverflow.com/questions/32090535/hibernate-5-and-spring-generate-ddl-using-schemaexport
 * <p>前述のやり取りを見る限りでは Bean Validation よりも JPA のアノテーションで明示すべきとのことなので、適切な DDL を自動生成したいなら以下のような JPA 列定義で
 * 桁数等の指定を明示的におこなうようにしてください。 ( DDL を自動生成しないのであれば気にしないで OK )
 * <pre>
 *  e.g. @Column(length = 32, nullable = true)
 * </pre>
 */
public class DdlExporter {

    private static final String PackageRoot = "sample";
    private static final String PackageDefault = PackageRoot + ".model";
    private static final String PackageSystem = PackageRoot + ".context";
    private static final String OrmDialect = "org.hibernate.dialect.H2Dialect";
    private static final String OutputRoot = "build/";
    private static final boolean FormatSql = false;

    public static void main(String[] args) {
        DdlExporter exporter = new DdlExporter();
        exporter.outputDdl(PackageSystem, OrmDialect, "ddl-system.sql");
        exporter.outputDdl(PackageDefault, OrmDialect, "ddl-default.sql");
    }

    private void outputDdl(String packageName, String dialect, String fileName) {
        LocalSessionFactoryBean sfBean = sfBean(packageName, dialect);
        StandardServiceRegistry serviceRegistry = sfBean.getConfiguration().getStandardServiceRegistryBuilder().build();
        try {
            String outputFile = OutputRoot + fileName;
            Files.deleteIfExists(Paths.get(outputFile));
            Metadata metadata = metadata(serviceRegistry, sfBean.getMetadataSources());
            
            SchemaExport export = new SchemaExport();
            export.setDelimiter(";");
            export.setFormat(FormatSql);
            export.setOutputFile(outputFile);
            export.create(EnumSet.of(TargetType.SCRIPT, TargetType.STDOUT), metadata);
        } catch (Exception e) {
            throw new InvocationException(e);
        } finally {
            StandardServiceRegistryBuilder.destroy(serviceRegistry);
        }
    }

    private LocalSessionFactoryBean sfBean(String packageName, String dialect) {
        LocalSessionFactoryBean sfBean = new LocalSessionFactoryBean();
        try {
            sfBean.setPackagesToScan(packageName);
            sfBean.setHibernateProperties(hibernateProperties(dialect));
            sfBean.afterPropertiesSet();
        } catch (IOException e) {
            throw new InvocationException(e);
        }
        return sfBean;
    }

    private Properties hibernateProperties(String dialect) {
        Properties props = new Properties();
        props.put("hibernate.dialect", dialect);
        props.put("hibernate.show_sql", false);
        props.put("hibernate.hbm2ddl.auto", "none");
        return props;
    }

    private Metadata metadata(StandardServiceRegistry serviceRegistry, MetadataSources metadataSources)
            throws Exception {
        MetadataBuilder builder = new MetadataBuilderImpl(metadataSources, serviceRegistry);
        Metadata metadata = builder
                .applyPhysicalNamingStrategy(new SpringPhysicalNamingStrategy())
                .applyImplicitNamingStrategy(new SpringImplicitNamingStrategy())
                .build();
        return metadata;
    }

}

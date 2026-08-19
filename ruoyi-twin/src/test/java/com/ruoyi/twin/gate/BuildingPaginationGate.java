package com.ruoyi.twin.gate;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.sql.DataSource;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.github.pagehelper.PageInterceptor;
import com.ruoyi.twin.building.entity.BuildingDO;
import com.ruoyi.twin.building.mapper.BuildingMapper;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

/**
 * Gate 2：验证 PageHelper 对建筑 PostGIS 查询的分页和总数改写。
 */
public final class BuildingPaginationGate {

    private static final int PAGE_SIZE = 20;

    private BuildingPaginationGate() {
    }

    public static void main(String[] args) throws Exception {
        String password = System.getenv("PGPASSWORD");
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("缺少环境变量 PGPASSWORD");
        }

        DataSource dataSource = new UnpooledDataSource(
                "org.postgresql.Driver",
                "jdbc:postgresql://localhost:5434/ry?currentSchema=public&stringtype=unspecified",
                "twin",
                password);
        SqlSessionFactory sessionFactory = createSessionFactory(dataSource);
        long expectedTotal = countBuildings(dataSource);

        try (SqlSession session = sessionFactory.openSession()) {
            BuildingMapper mapper = session.getMapper(BuildingMapper.class);

            PageHelper.startPage(1, PAGE_SIZE);
            List<BuildingDO> firstPage = mapper.selectBuildingPage(null, null);
            PageInfo<BuildingDO> firstPageInfo = new PageInfo<>(firstPage);

            PageHelper.startPage(2, PAGE_SIZE);
            List<BuildingDO> secondPage = mapper.selectBuildingPage(null, null);
            PageInfo<BuildingDO> secondPageInfo = new PageInfo<>(secondPage);

            require(firstPageInfo.getTotal() == expectedTotal,
                    "分页总数不正确：" + firstPageInfo.getTotal() + " != " + expectedTotal);
            require(firstPage.size() == PAGE_SIZE, "第一页条数不正确：" + firstPage.size());
            require(secondPage.size() == PAGE_SIZE, "第二页条数不正确：" + secondPage.size());
            require(secondPageInfo.getPageNum() == 2, "第二页页码不正确：" + secondPageInfo.getPageNum());

            Set<Long> firstPageIds = new HashSet<>();
            firstPage.forEach(building -> firstPageIds.add(building.getId()));
            require(secondPage.stream().noneMatch(building -> firstPageIds.contains(building.getId())),
                    "第一页与第二页存在重复 ID");
            require(firstPage.get(PAGE_SIZE - 1).getId() < secondPage.get(0).getId(),
                    "第二页没有延续第一页的 ID 顺序");

            System.out.printf("GATE2_PASS total=%d page1=%d page2=%d firstPageLastId=%d secondPageFirstId=%d%n",
                    firstPageInfo.getTotal(), firstPage.size(), secondPage.size(),
                    firstPage.get(PAGE_SIZE - 1).getId(), secondPage.get(0).getId());
        }
    }

    private static SqlSessionFactory createSessionFactory(DataSource dataSource) throws Exception {
        Environment environment = new Environment("gate2", new JdbcTransactionFactory(), dataSource);
        Configuration configuration = new Configuration(environment);
        configuration.setMapUnderscoreToCamelCase(true);

        PageInterceptor pageInterceptor = new PageInterceptor();
        Properties properties = new Properties();
        properties.setProperty("helperDialect", "postgresql");
        pageInterceptor.setProperties(properties);
        configuration.addInterceptor(pageInterceptor);

        try (InputStream inputStream = Resources.getResourceAsStream("mapper/twin/BuildingMapper.xml")) {
            XMLMapperBuilder mapperBuilder = new XMLMapperBuilder(
                    inputStream, configuration, "mapper/twin/BuildingMapper.xml", configuration.getSqlFragments());
            mapperBuilder.parse();
        }
        return new SqlSessionFactoryBuilder().build(configuration);
    }

    private static long countBuildings(DataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT count(*) FROM t_building")) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}

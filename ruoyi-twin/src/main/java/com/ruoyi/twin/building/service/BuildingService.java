package com.ruoyi.twin.building.service;

import java.util.List;

import tools.jackson.databind.JsonNode;
import com.ruoyi.twin.building.dto.BuildingPageQuery;
import com.ruoyi.twin.building.dto.BuildingSaveDTO;
import com.ruoyi.twin.building.vo.BuildingDetailVO;
import com.ruoyi.twin.building.vo.BuildingPageVO;
import com.ruoyi.twin.building.vo.TilesetInfoVO;
import com.ruoyi.twin.common.result.PageResult;

/**
 * 建筑服务
 *
 * @author lvfan
 */
public interface BuildingService {

    /**
     * 获取 3D Tiles 地址与初始视角
     *
     * @return tilesetUrl 未配置时为 null
     */
    TilesetInfoVO getTilesetInfo();

    /**
     * 查询建筑详情
     *
     * @param id 主键
     * @return 建筑详情
     * @throws com.ruoyi.twin.common.exception.BizException 建筑不存在时抛出
     */
    BuildingDetailVO getDetail(Long id);

    /**
     * 查询建筑轮廓 GeoJSON
     *
     * @param bbox 视口范围，格式 west,south,east,north，为空表示全域
     * @return GeoJSON FeatureCollection
     * @throws com.ruoyi.twin.common.exception.BizException bbox 格式或取值非法时抛出
     */
    JsonNode getGeoJson(String bbox);

    /**
     * 建筑分页查询，供管理端列表使用
     *
     * @param query 分页与筛选参数
     * @return 当页建筑，含中心点经纬度
     */
    PageResult<BuildingPageVO> pageBuildings(BuildingPageQuery query);

    /**
     * 查询建筑列表，由若依管理页在 Controller 层启动 PageHelper 分页
     *
     * @param query 筛选参数
     * @return 建筑列表
     */
    List<BuildingPageVO> listBuildings(BuildingPageQuery query);

    /**
     * 新增建筑，footprint 取中心点向四周扩张的近似矩形
     *
     * @param dto 建筑入参
     * @return 新建筑主键
     * @throws com.ruoyi.twin.common.exception.BizException 高度来源非法时抛出
     */
    Long createBuilding(BuildingSaveDTO dto);

    /**
     * 整体更新建筑表单可编辑字段，几何按新中心点重建
     *
     * @param id  建筑主键
     * @param dto 建筑入参
     * @throws com.ruoyi.twin.common.exception.BizException 建筑不存在或高度来源非法时抛出
     */
    void updateBuilding(Long id, BuildingSaveDTO dto);

    /**
     * 删除建筑。建筑下仍有设备时拒绝删除，引用完整性由应用层保证
     *
     * @param id 建筑主键
     * @throws com.ruoyi.twin.common.exception.BizException 建筑不存在或仍有关联设备时抛出
     */
    void deleteBuilding(Long id);
}

package cn.wanghaomiao.seimi.crawlers;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import cn.wanghaomiao.seimi.annotation.Crawler;
import cn.wanghaomiao.seimi.def.BaseSeimiCrawler;
import cn.wanghaomiao.seimi.struct.Request;
import cn.wanghaomiao.seimi.struct.Response;
import cn.wanghaomiao.seimi.utils.JsonUtils;

import java.util.List;

/**
 * test 类
 *
 * @description:
 * @author: Longlive
 * @since: 2018年09月13日 12:34 AM
 */
@Crawler(name = "jianlang")
public class JianLangDatas extends BaseSeimiCrawler {

    private int pageNo = 1;
    private int pageSize = 1;//134

    private String urlTemp = "http://www.zsjlyp.com/product/list?is_json=1&page={}";

    private List<Dict> lists = CollUtil.newArrayList();

    @Override
    public String[] startUrls() {
        return new String[]{StrFormatter.format(urlTemp, pageNo)};
    }

    @Override
    public void start(Response response) {
        String content = response.getContent();
        JSONArray models = (JSONArray) JSONUtil.parse(content).getByPath("models");
        for (Object model : models) {
            Dict base = JsonUtils.getBean(model.toString(), Dict.class);
            Dict product = JsonUtils.getBean(base.getStr("product"), Dict.class);

            lists.add(Dict.create()
                    .set("序号(保留)", base.getStr("id"))
                    .set("bar_code(保留)", base.getStr("bar_code"))
                    .set("batch_code(保留)", base.getStr("batch_code"))
                    .set("通用名", product.getStr("general_name"))
                    .set("生产商", product.getStr("produce_unit"))
                    .set("规  格", base.getStr("norms"))
                    .set("库存", base.getStr("stock"))
                    .set("价格1", base.getStr("price"))
                    .set("价格2", base.getStr("jiagete"))
                    .set("有效期", base.getStr("last_date")));
        }

        pageNo++;
        if(pageNo <= pageSize) {
            push(Request.build(StrFormatter.format(urlTemp, pageNo), JianLangDatas::start));
        }else {
            int columnNums = 10;
            String now = DateUtil.today();
            // 通过工具类创建writer
            ExcelWriter writer = ExcelUtil.getWriter("G:/中山健朗药品报货平台" + now + ".xlsx");
            // 合并单元格后的标题行，使用默认标题样式
            writer.merge(columnNums - 1, "中山健朗药品报货平台" + now);

            // 一次性写出内容，使用默认样式
            writer.write(lists);

            for (int i = 0; i < columnNums; i++) {
                writer.autoSizeColumn(i);
            }

            // 关闭writer，释放内存
            writer.close();
            System.out.println("完成导出>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        }
    }
}

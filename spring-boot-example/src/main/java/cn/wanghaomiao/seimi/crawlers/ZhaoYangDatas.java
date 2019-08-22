package cn.wanghaomiao.seimi.crawlers;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.text.StrFormatter;
import cn.wanghaomiao.seimi.annotation.Crawler;
import cn.wanghaomiao.seimi.def.BaseSeimiCrawler;
import cn.wanghaomiao.seimi.struct.Request;
import cn.wanghaomiao.seimi.struct.Response;
import org.seimicrawler.xpath.JXDocument;

import java.util.List;

/**
 * ZhaoYangDatas 类
 *
 * @description: 朝阳药业数据抓取
 * @author: Longlive
 * @since: 2018年09月13日 6:10 PM
 */
@Crawler(name = "zhaoyang")
public class ZhaoYangDatas extends BaseSeimiCrawler {

    String hostTemp = "http://yun.sunus-china.com/{}";

    int categoryNum = 0;
    List<Dict> categorys = CollUtil.newArrayList();

    @Override
    public String[] startUrls() {

        return new String[]{StrFormatter.format(hostTemp, "category.php")};
    }

    @Override
    public void start(Response response) {
        JXDocument doc = response.document();
        List<Object> categoryUrlList = doc.sel("//h3/a/@href");
        categoryNum = categoryUrlList.size();
        for (Object s : categoryUrlList) {
            //"&display=text&brand=0&price_min=0&price_max=0&filter_attr=0&page={}&sort=last_update&order=DESC#goods_list"
            push(Request.build(StrFormatter.format(hostTemp, s.toString()), ZhaoYangDatas::getCategoryData));
        }
    }

    /**
     * 获取分类页属性（url/pageNo/pageSize）
     * @param response
     */
    public void getCategoryData(Response response) {
        JXDocument doc = response.document();

        int totle = Convert.toInt(doc.selOne("//div[@id='pager']/b/text()"), 1);

        categorys.add(Dict.create()
                .set("url", StrFormatter.format("{}&display=text&brand=0&price_min=0&price_max=0&filter_attr=0&page={}&sort=last_update&order=DESC#goods_list", response.getUrl()))
                .set("pageNo", 1)
                .set("pageSize", (totle / 12) + 1));

        //表示已经采集完成
        if(categorys.size() == categoryNum) {
            for (Dict category : categorys) {
                
            }
        }
    }

    public void getGoodList() {

    }

}

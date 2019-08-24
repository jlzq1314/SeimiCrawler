package cn.wanghaomiao.seimi.crawlers.amazon;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.StrUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import cn.wanghaomiao.seimi.annotation.Crawler;
import cn.wanghaomiao.seimi.def.BaseSeimiCrawler;
import cn.wanghaomiao.seimi.http.HttpMethod;
import cn.wanghaomiao.seimi.struct.Request;
import cn.wanghaomiao.seimi.struct.Response;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.seimicrawler.xpath.JXDocument;

import java.util.List;

/**
 * AmazonDatas 类
 *
 * @description: Amazon数据抓取
 * @author: Longlive-n
 * @since: 2019年08月24日 10:53
 */
@Crawler(name = "amazon")
public class AmazonDatas extends BaseSeimiCrawler {

    String urlTemp = "https://www.amazon.com/gp/new-releases/books/283155/ref=zg_bsnr_pg_{}?ie=UTF8&pg={}";

    List<Dict> lists = CollUtil.newArrayList();

    // 当前分页数
    int page = 1;
    // 分页 2
    int pageSize = 2;
    // Excel列数
    int excelColumnNums = 1;

    @Override
    public String[] startUrls() {
        return null;
    }

    @Override
    public List<Request> startRequests() {
        List<Request> requests = CollUtil.newArrayList();
        for (int i = 1; i <= pageSize; i++) {
            Request request = Request.build(StrFormatter.format(urlTemp, i, i), AmazonDatas::start);
            request.setHttpMethod(HttpMethod.GET);
            requests.add(request);
        }
        return requests;
    }

    @Override
    public void start(Response response) {
        JXDocument doc = response.document();

        for (Object s : doc.sel("//ol[@id='zg-ordered-list']/li[@class='zg-item-immersion']")) {
            Dict msgDict = Dict.create();
            JXDocument panelInfo = JXDocument.create(s + "");

            msgDict.set("NO.", StrUtil.toString(panelInfo.selOne("//div[@class='a-row a-spacing-none aok-inline-block']/span//text()")))
                    .set("Link", "https://www.amazon.com" + panelInfo.selOne("//a[@class='a-link-normal']/@href"))
                    .set("Name", panelInfo.selOne("//a[@class='a-link-normal']//text()"))
                    .set("Image", panelInfo.selOne("//img/@src"));

            List<Object> itemText = panelInfo.sel("//span[@class='aok-inline-block zg-item']/div//text()");
            if (itemText.size() > 4) {
                msgDict.set("Author", itemText.get(0))
                    .set("Stars", itemText.get(1))
                    .set("Secondary", itemText.get(2))
                    .set("Price", itemText.get(3))
                    .set("ReleaseDate", itemText.get(4));
            } else {
                msgDict.set("Author", itemText.get(0))
                    .set("Stars", "")
                    .set("Secondary", itemText.get(1))
                    .set("Price", itemText.get(2))
                    .set("ReleaseDate", itemText.get(3));
            }

            excelColumnNums = msgDict.size();
            lists.add(msgDict);
        }

        // 开始导出
        if (pageSize == page) {
            excelExport();
        } else {
            page++;
        }
    }

    public void excelExport() {
        System.out.println("导出初始化>>>>>>>>>>>>>>>>>>>>>>>" + lists.size());
        String now = DateUtil.today();
        // 通过工具类创建writer
        ExcelWriter writer = ExcelUtil.getWriter("D:/ZXSCIT/Amazon@" + now + ".xlsx");
        // 合并单元格后的标题行，使用默认标题样式
        writer.merge(excelColumnNums - 1, "New Releases in Books Datas For Amazon");

        Font headFont = writer.createFont();
        headFont.setBold(true);
        headFont.setFontHeightInPoints((short) 16);

        CellStyle headCellStyle = writer.getHeadCellStyle();
        headCellStyle.setFont(headFont);

        writer.setRowHeight(0, 28);

        writer.autoSizeColumnAll();

         // 一次性写出内容，使用默认样式
        writer.write(lists, true);

        // 关闭writer，释放内存
        writer.close();
        System.out.println("完成导出>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>" + lists.size());
    }
}

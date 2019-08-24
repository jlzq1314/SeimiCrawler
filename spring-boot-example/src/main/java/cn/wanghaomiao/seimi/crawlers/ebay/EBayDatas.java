package cn.wanghaomiao.seimi.crawlers.ebay;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
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
 * EBayDatas 类
 *
 * @description: EBay 数据采集
 * @author: Longlive-n
 * @since: 2019年08月24日 15:49
 */
@Crawler(name = "ebay")
public class EBayDatas extends BaseSeimiCrawler {

    String urlTemp = "https://www.ebay.com/b/Apple-iPhone/9355/bn_319682?_pgn={}";

    List<Dict> lists = CollUtil.newArrayList();

    // 当前分页数
    int page = 1;
    // 分页 3
    int pageSize = 3;
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
            Request request = Request.build(StrFormatter.format(urlTemp, i), EBayDatas::start);
            request.setHttpMethod(HttpMethod.GET);
            requests.add(request);
        }
        return requests;
    }

    @Override
    public void start(Response response) {
        JXDocument doc = response.document();

        for (Object s : doc.sel("//ul[@class='b-list__items_nofooter']/li")) {
            Dict msgDict = Dict.create();
            JXDocument panelInfo = JXDocument.create(s + "");

            msgDict.set("Title", panelInfo.selOne("//h3[@class='s-item__title']//text()"))
                    .set("Sub-Title", Convert.toStr(panelInfo.selOne("//div[@class='s-item__subtitle']//text()"), ""))
                    .set("Detail-Link", panelInfo.selOne("//div[@class='s-item__image']/a/@href"))
                    .set("Image-Link", panelInfo.selOne("//div[@class='s-item__image-wrapper']/img/@src"));

            String price = "";
            StringBuilder remark = StrUtil.builder();
            List<Object> itemList = panelInfo.sel("//div[@class='s-item__details clearfix']/*[starts-with(@class,'s-item__detail')]//text()");
            for (int i = 0; i < itemList.size(); i++) {
                String itemText = Convert.toStr(itemList.get(i), "");
                if (StrUtil.isBlank(itemText) || "Watch".equals(itemText)) {
                    continue;
                }

                if (itemText.contains("eBay determines")) {
                    itemText = StrUtil.subBefore(itemText, "eBay determines", true);
                }

                // Price
                if (itemText.startsWith("RMB")) {
                    price = itemText;
                    continue;
                }

                // Remarks
                remark.append(itemText);
                if (i < itemList.size() - 1) {
                    remark.append(StrUtil.CRLF);
                }
            }
            msgDict.set("Price", price);
            msgDict.set("Remarks", remark.toString());

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
        ExcelWriter writer = ExcelUtil.getWriter("D:/ZXSCIT/Ebay@" + now + ".xlsx");
        // 合并单元格后的标题行，使用默认标题样式
        writer.merge(excelColumnNums - 1, "Apple iPhone Datas For Ebay");

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

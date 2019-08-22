package cn.wanghaomiao.seimi.crawlers;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import cn.wanghaomiao.seimi.annotation.Crawler;
import cn.wanghaomiao.seimi.def.BaseSeimiCrawler;
import cn.wanghaomiao.seimi.http.HttpMethod;
import cn.wanghaomiao.seimi.struct.Request;
import cn.wanghaomiao.seimi.struct.Response;
import org.seimicrawler.xpath.JXDocument;
import org.seimicrawler.xpath.JXNode;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * JiaYouGanDatas 类
 *
 * @author Longlive
 * @description: 加油干数据抓取
 * @since 2018年11月28日 9:10 PM
 */
@Crawler(name = "jiayougan", delay = 1, httpTimeOut = 360000, useCookie = true)
public class JiaYouGanDatas extends BaseSeimiCrawler {

    //新用户名linshi密码linshi0595
    String urlTemp = "http://hicomeon.net/web/index.php";

    Map<String, String> headers = CollUtil.newHashMap();

    List<Dict> lists = CollUtil.newArrayList();

    //分页 34
    int pageSize = 34;
    // 商品条数
    int goodNums = 0;

    //Excel列数
    int excelColumnNums = 44;

    @Override
    public String[] startUrls() {
        return null;
    }

    @Override
    public List<Request> startRequests() {
        headers.put("Cookie", "UM_distinctid=1675a64eddc4ae-0ae94c2fb12d21-4313362-1fa400-1675a64eddd761; CNZZDATA1255882846=1421454170-1494922785-%7C1494922785; gr_user_id=7f5e557f-5979-4e95-aee0-d7b5adc65721; gr_session_id_a4dc7294de4088e6=fcc95187-c110-418c-84af-f7b108c02eb3; gr_session_id_a4dc7294de4088e6_fcc95187-c110-418c-84af-f7b108c02eb3=false; dc6f___session=eyJ1aWQiOiIxNiIsImxhc3R2aXNpdCI6IjE1NDQwNTgxNDciLCJsYXN0aXAiOiIxMTAuODEuMTI3LjE0MCIsImhhc2giOiI2NzYyNDg3YjBiZTE3ZDc5Yjk0NjhiZmJkYWJjZTFjYiJ9; dc6f___switch=ZRiUr; dc6f___uniacid=2; dc6f___uid=16; dc6f_history_url=%5B%7B%22title%22%3A%22%5Cu5546%5Cu54c1-%5Cu51fa%5Cu552e%5Cu4e2d%22%2C%22url%22%3A%22.%5C%2Findex.php%3Fc%3Dsite%26a%3Dentry%26m%3Dewei_shopv2%26do%3Dweb%26r%3Dgoods%22%7D%5D");

        List<Request> requests = new LinkedList<>();

        List<String> urls = CollUtil.newArrayList();
        for (int i = 1; i <= pageSize; i++) {
            urls.add(StrFormatter.format("http://hicomeon.net/web/index.php?c=site&a=entry&m=ewei_shopv2&do=web&r=goods&page={}", i));
        }

        for (String s : urls) {
            Request request = Request.build(s, JiaYouGanDatas::start);
            request.setHttpMethod(HttpMethod.GET);
            request.setHeader(headers);
            requests.add(request);
        }

        return requests;
    }

    @Override
    public void start(Response response) {
        JXDocument doc = response.document();

        List<Object> urls = doc.sel("//div[@class='page-content']/div[@class='row']/div[@class='col-md-12']/table[@class='table table-responsive']/tbody/tr/td[3]/a/@href");
        if (CollUtil.isNotEmpty(urls)) {
            //获取每页商品的详情链接
            for (Object s : urls) {
                goodNums++;
                System.out.println(StrFormatter.format("{}---{}", goodNums, s));
                Request request = Request.build(Convert.toStr(s), JiaYouGanDatas::start);
                request.setHttpMethod(HttpMethod.GET);
                request.setHeader(headers);
                push(request);
            }
        } else {
            //商品面版信息
            Dict msgDict = Dict.create();

            //商品Url http://hicomeon.net/web/index.php?c=site&a=entry&m=ewei_shopv2&do=web&r=goods.edit&id=215&goodsfrom=sale
            String url = response.getRealUrl();
            msgDict.set("商品ID", StrUtil.subBetween(url, "id=", "&"));

            //商户名称
            List<JXNode> shopNode = doc.selN("//div[@class='page-header']/span[@class='text-primary']/small/span//text()");
            String shopName = "";
            if(CollUtil.isNotEmpty(shopNode) && shopNode.size() > 1) {
                shopName = shopNode.get(1).toString();
            }
            msgDict.set("商户名称", shopName);

            //面板--详情
            Object detailNode = doc.selOne("//textarea[@id='detail']//text()");
            msgDict.set("商品详情", StrUtil.subBetween(detailNode.toString(), "img src=\"", "\" width="));

            //面板--基本
            for (Object s : doc.selN("//div[@id='tab_basic']/div[@class='panel-body']//div[@class='region-goods-right col-sm-10']/div[starts-with(@class, 'form-group')]")) {
                if (ObjectUtil.isNull(s)) {
                    continue;
                }

                JXDocument panelInfo = JXDocument.create(s + "");
                Object fgNode = panelInfo.selOne("//div[starts-with(@class, 'form-group') and contains(@style, 'display: none;')]]");
                if (ObjectUtil.isNotNull(fgNode)) {
                    continue;
                }

                String label = Convert.toStr(panelInfo.selOne("//div[starts-with(@class, 'form-group')]//label[contains(@class, 'control-label')]//text()"));
                if (StrUtil.isBlankOrUndefined(label) || "首图视频".equals(label)) {
                    continue;
                }

                String val = Convert.toStr(panelInfo.selOne("//div[starts-with(@class, 'form-group')]//div[contains(@class, 'form-control-static')]//text()"), "");
                if (StrUtil.isBlankOrUndefined(val)) {
                    //获取图片节点
                    val = Convert.toStr(panelInfo.selOne("//div[starts-with(@class, 'form-group')]//div[contains(@class, 'gimgs')]/a/@href"), "");
                }

                msgDict.set(label, val);
            }

            //面板--库存/价格
            for (Object s : doc.selN("//div[@id='tab_option']/div[@class='panel-body']//div[@class='region-goods-right col-sm-10']/div[starts-with(@class, 'form-group')]")) {
                if (ObjectUtil.isNull(s)) {
                    continue;
                }

                JXDocument panelInfo = JXDocument.create(s + "");
                Object fgNode = panelInfo.selOne("//div[starts-with(@class, 'form-group') and contains(@style, 'display: none;')]]");
                if (ObjectUtil.isNotNull(fgNode)) {
                    continue;
                }

                String label = Convert.toStr(panelInfo.selOne("//div[starts-with(@class, 'form-group')]//label[contains(@class, 'control-label')]//text()"));
                if (StrUtil.isBlankOrUndefined(label)) {
                    continue;
                }

                String val = Convert.toStr(panelInfo.selOne("//div[starts-with(@class, 'form-group')]//div[contains(@class, 'form-control-static')]//text()"), "");
                if (StrUtil.contains(val, Convert.toChar("显示库存"))) {
                    val = StrUtil.replace(val, "显示库存", "");
                }
                msgDict.set(label, val);

                if ("编码".equals(label)) {
                    String label1 = Convert.toStr(panelInfo.sel("//div[starts-with(@class, 'form-group')]//label[contains(@class, 'control-label')]//text()").get(1));
                    String val1 = Convert.toStr(panelInfo.sel("//div[starts-with(@class, 'form-group')]//div[contains(@class, 'form-control-static')]//text()").get(1), "");
                    msgDict.set(label1, val1);
                }
            }

            //面板--参数
            for (JXNode node : doc.selN("//div[@id='tab_param']/div[@class='panel-body']//tbody[@id='param-items']//tr")) {
                if (ObjectUtil.isNull(node)) {
                    continue;
                }

                List<JXNode> trNode = node.sel("/td//text()");
                msgDict.set(trNode.get(0).toString(), trNode.get(1).toString());
            }

            lists.add(msgDict);

            System.out.println(StrFormatter.format("商品条数：{}---数据采集数：{}", goodNums, lists.size()));
            if (lists.size() == goodNums) {
                excelExport();
            }
        }
    }

    public void excelExport() {
        System.out.println("导出初始化>>>>>>>>>>>>>>>>>>>>>>>" + lists.size());
        String now = DateUtil.today();
        // 通过工具类创建writer
        ExcelWriter writer = ExcelUtil.getWriter("D:/ZXSCIT/嗨加油干平台" + now + ".xlsx");
        // 合并单元格后的标题行，使用默认标题样式
        writer.merge(excelColumnNums - 1, "嗨加油干平台" + now);

        // 一次性写出内容，使用默认样式
        writer.write(lists);

        for (int i = 0; i < excelColumnNums; i++) {
            writer.autoSizeColumn(i);
        }

        // 关闭writer，释放内存
        writer.close();
        System.out.println("完成导出>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>" + lists.size());
    }
}

package cn.wanghaomiao.seimi.http;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import jodd.io.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * test 类
 *
 * @description:
 * @author: Longlive
 * @since: 2019年03月15日 下午 9:27
 */
public class test {

     static final String filePathTemp = "D:/ZXSCIT/111/aaa-{}.xlsx";
     static final int fileSize = 5;
     static final int columnSize = 12;

    public static void main(String[] args) {
        List<Map<String, Object>> rowsList = CollUtil.newArrayList();
        for (int i = 1; i <= fileSize; i++) {
            ExcelReader reader = ExcelUtil.getReader(StrFormatter.format(filePathTemp, (i + "-new")));
            rowsList.addAll(reader.readAll());
        }
        excelExport(rowsList);
    }

    public static void excelExport(List<Map<String, Object>> excelDatas) {
        if (CollUtil.isEmpty(excelDatas)) {
            return;
        }
        System.out.println("导出初始化>>>>>>>>>>>>>>>>>>>>>>>" + excelDatas.size());
        String now = DateUtil.today();
        String fileName = StrFormatter.format(filePathTemp, now);
        // 判断文件是否存在，存在删除后再写入
        if (FileUtil.isExistingFile(new File(fileName))) {
            try {
                FileUtil.delete(fileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // 通过工具类创建writer
        ExcelWriter writer = ExcelUtil.getWriter(fileName);
        // 设置单元格强制换行
        writer.getStyleSet().getCellStyle().setWrapText(true);
        // 合并单元格后的标题行，使用默认标题样式
        writer.merge(columnSize - 1, "陈星海" + now);
        for (int i = 0; i < columnSize; i++) {
            writer.autoSizeColumn(i);
        }
        // 云药库
        writer.setColumnWidth((columnSize - 2), 80);
        // 广州医
        writer.setColumnWidth((columnSize - 3), 80);
        // 九州通
        writer.setColumnWidth((columnSize - 4), 80);
        // 生产企业名称
        writer.setColumnWidth((columnSize - 6), 35);
        // 通用名称
        writer.setColumnWidth((columnSize - 10), 15);
        // 药品名称
        writer.setColumnWidth((columnSize - 11), 18);

        // 一次性写出内容，使用默认样式
        writer.write(excelDatas, true);
        // 关闭writer，释放内存
        writer.close();
        System.out.println("完成导出>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>" + excelDatas.size());
    }

}

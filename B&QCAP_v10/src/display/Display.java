package display;

import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import core.GeneticAlgorithm;
import jxl.JXLException;
import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;

public class Display {

    private static GeneticAlgorithm geneticAlgorithm = new GeneticAlgorithm();

    public static void main(String[] args) throws JXLException, IOException {

        geneticAlgorithm.start();
        chartDisplay();// 折线图展示
        createExcel();// 输出Excel
    }

    // 折线图展示
    private static void chartDisplay() {

        StandardChartTheme myChartTheme = new StandardChartTheme("CN");
        myChartTheme.setLargeFont(new Font("楷体", Font.PLAIN, 15));
        myChartTheme.setExtraLargeFont(new Font("黑体", Font.PLAIN, 18));
        myChartTheme.setRegularFont(new Font("楷体", Font.BOLD, 15));
        ChartFactory.setChartTheme(myChartTheme);

        XYSeriesCollection myCollection = getCollection();

        JFreeChart myChart = ChartFactory.createXYLineChart("船舶在港时间趋势图", "种群迭代次数/次", "在港时间/h", myCollection,
                PlotOrientation.VERTICAL, true, true, false);

        ChartFrame myChartFrame = new ChartFrame("船舶在港时间趋势图", myChart);
        myChartFrame.pack();
        myChartFrame.setVisible(true);
        myChartFrame.setLocationRelativeTo(null);
    }

    // 输出Excel
    private static void createExcel() throws IOException, JXLException {

        WritableWorkbook book = Workbook.createWorkbook(new File("plan.xls"));// 创建文件

        // 生成名为“最优解”的工作表，参数0表示这是第一页
        WritableSheet sheet = book.createSheet("最优解", 0);

        // 将单元格添加到工作表中
        sheet.addCell(new Label(0, 0, "船舶编号 S"));
        sheet.addCell(new Label(0, 1, "靠泊顺序集 SO"));
        sheet.addCell(new Label(0, 2, "靠泊泊位集 SB"));
        sheet.addCell(new Label(0, 3, "分配岸桥集 SC"));
        sheet.addCell(new Label(0, 4, "平均在港时间 /h"));

        sheet.setColumnView(0, 15);// 第一列设置列宽15

        int[] gene_shipOrder = geneticAlgorithm.getOptimalChromosomeShipOrder();
        int[] gene_shipBerth = geneticAlgorithm.getOptimalChromosomeShipBerth();
        int[] gene_shipCrane = geneticAlgorithm.getOptimalChromosomeShipCrane();
        double score = geneticAlgorithm.getOptimalChromosomeScore();

        int length = gene_shipOrder.length;

        // 设置单元格-船舶编号
        for (int i = 0; i < length; i++) {
            jxl.write.Number num = new jxl.write.Number(i + 1, 0, i + 1);
            sheet.addCell(num);
        }

        // 设置单元格-靠泊泊位集
        for (int i = 0; i < length; i++) {
            jxl.write.Number num = new jxl.write.Number(i + 1, 1, gene_shipOrder[i]);
            sheet.addCell(num);
        }

        // 设置单元格-靠泊泊位集
        for (int i = 0; i < length; i++) {
            jxl.write.Number num = new jxl.write.Number(i + 1, 2, gene_shipBerth[i]);
            sheet.addCell(num);
        }

        // 设置单元格-分配岸桥集
        for (int i = 0; i < length; i++) {
            jxl.write.Number num = new jxl.write.Number(i + 1, 3, gene_shipCrane[i]);
            sheet.addCell(num);
        }

        // 设置单元格-在港时间
        jxl.write.Number num = new jxl.write.Number(1, 4, 1 / score);
        sheet.addCell(num);

        // 写入数据并关闭文件
        book.write();
        book.close();

    }

    // 生成折线图
    private static XYSeriesCollection getCollection() {

        XYSeriesCollection myCollection = new XYSeriesCollection();

        XYSeries mySeriesBest = new XYSeries("最好值");
        XYSeries mySeriesAverage = new XYSeries("平均值");
        XYSeries mySeriesWorst = new XYSeries("最差值");

        Map<Integer, Double> bestMap = geneticAlgorithm.getBestMap();
        Map<Integer, Double> averageMap = geneticAlgorithm.getAverageMap();
        Map<Integer, Double> worstMap = geneticAlgorithm.getWorstMap();

        Set<Entry<Integer, Double>> bestEntrySet = bestMap.entrySet();
        Set<Entry<Integer, Double>> averageEntrySet = averageMap.entrySet();
        Set<Entry<Integer, Double>> worstEntrySet = worstMap.entrySet();

        for (Entry<Integer, Double> entry : bestEntrySet) {
            mySeriesBest.add(entry.getKey(), entry.getValue());
        }

        for (Entry<Integer, Double> entry : averageEntrySet) {
            mySeriesAverage.add(entry.getKey(), entry.getValue());
        }

        for (Entry<Integer, Double> entry : worstEntrySet) {
            mySeriesWorst.add(entry.getKey(), entry.getValue());
        }

        myCollection.addSeries(mySeriesAverage);
        myCollection.addSeries(mySeriesBest);
        myCollection.addSeries(mySeriesWorst);

        return myCollection;
    }

}
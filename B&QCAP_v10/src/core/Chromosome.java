package core;

public class Chromosome {
    private int[] gene_shipOrder;// 基因序列1 -> 靠泊顺序集
    private int[] gene_shipBerth;// 基因序列2 -> 靠泊泊位集
    private int[] gene_shipCrane;// 基因序列3 -> 分配岸桥集
    private double score;// 目标函数值，这里是平均在港时间的倒数

    // 按照基因序列长 -> 船舶数量构造Chromosome对象
    public Chromosome(int ship_num, int berth_num, int[] craneMax) {
        if (ship_num > 0) {
            generateShipOrder(ship_num);
            generateShipBerth(ship_num, berth_num);
            generateShipCrane(ship_num, craneMax);

        } else {
            System.out.println("基因序列大小不能小于1！");
            System.exit(-1);
        }
    }

    public Chromosome() {

    }

    public Chromosome(int[] gene_shipOrder, int[] gene_shipBerth, int[] gene_shipCrane) {
        super();
        this.gene_shipOrder = gene_shipOrder;
        this.gene_shipBerth = gene_shipBerth;
        this.gene_shipCrane = gene_shipCrane;
    }

    // 初始化靠泊顺序集
    private void generateShipOrder(int ship_num) {
        this.gene_shipOrder = new int[ship_num];

        boolean[] array = new boolean[ship_num];// boolean[] array用于标识i是否已出现过
        for (int i = 0; i < array.length; i++) {
            array[i] = false;
        }

        for (int i = 0; i < ship_num; i++) {
            int temp = ((int) (Math.random() * ship_num)) % ship_num;

            while (array[temp]) {
                temp = ((int) (Math.random() * ship_num)) % ship_num;
            }

            gene_shipOrder[i] = temp + 1;
            array[temp] = true;

        }
    }

    // 初始化靠泊泊位集
    private void generateShipBerth(int ship_num, int berth_num) {
        this.gene_shipBerth = new int[ship_num];
        for (int i = 0; i < ship_num; i++) {
            int temp = (int) (Math.random() * berth_num + 1);
            gene_shipBerth[i] = temp;
        }
    }

    // 初始化分配岸桥集
    private void generateShipCrane(int ship_num, int[] craneMax) {
        this.gene_shipCrane = new int[ship_num];
        for (int i = 0; i < ship_num; i++) {
            int temp = (int) (Math.random() * craneMax[i] + 1);
            gene_shipCrane[i] = temp;
        }
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public int[] getGene_shipOrder() {
        return gene_shipOrder;
    }

    public int[] getGene_shipBerth() {
        return gene_shipBerth;
    }

    public int[] getGene_shipCrane() {
        return gene_shipCrane;
    }

    public void setGene_shipOrder(int[] gene_shipOrder) {
        this.gene_shipOrder = gene_shipOrder;
    }

    public void setGene_shipBerth(int[] gene_shipBerth) {
        this.gene_shipBerth = gene_shipBerth;
    }

    public void setGene_shipCrane(int[] gene_shipCrane) {
        this.gene_shipCrane = gene_shipCrane;
    }

}

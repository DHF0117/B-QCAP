package core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * 模拟遗传算法的过程，求解船舶在港时间的数学模型
 *
 * @author dinghj
 *
 */
public class GeneticAlgorithm {

    // 遗传算法相关参数
    private static final int ITERATION_MAX = 5000;// 最大种群迭代代数
    private static final int POPULATION_SIZE = 80;// 种群大小
    private static final double CROSS_RATE = 0.8;// 交叉率
    private static final double MUTATION_RATE = 0.1;// 变异率

    // 泊位岸桥分配问题相关参数
    private static final int[] craneMax = loadFileToArray("craneMax");// 能接受的最大岸桥数
    private static final int SHIP_NUM = craneMax.length;// 船舶数
    private static final int BERTH_NUM = 4;// 泊位数
    private static final double CRANE_EFFICIENCY = 0.55;// 岸桥工作效率
    private static final int INF = 10000;// 无限大的值

    private int generation;// 当前遗传代数
    private double bestScore;// 一次迭代中种群中的最好适应度值
    private double worstScore;// 一次迭代中种群中的最差适应度值
    private double time_average;// 染色体的平均在港时间的平均

    private double optimalScore = 0;// 遗传过程中的最好适应度值
    private int optimalGeneraion;// 遗传过程中最优解所在的遗传代数
    private Chromosome optimalChromosome;// 最后一代的最优染色体

    private List<Chromosome> population = new ArrayList<>();// 种群

    private Map<Integer, Double> bestMap = new HashMap<>();// 保存最好值数据的哈希表
    private Map<Integer, Double> averageMap = new HashMap<>();// 保存平均值数据的哈希表
    private Map<Integer, Double> worstMap = new HashMap<>();// 保存最差值数据的哈希表

    public void start() {

        // 1.初始化
        init();
        printCurrentGeneration();

        while (generation < ITERATION_MAX) {

            generation++;
            choose();// 2.选择
            cross();// 3.交叉
            mutation();// 4.变异

            for (Chromosome chro : population) {
                setChromosomeScore(chro);// 遗传完成后设置新的适应度值
            }

            calculatePopulationScore();// 5.计算新种群的适应度值
            printCurrentGeneration();
        }

        System.out.println("最后一代种群中最优的染色体：");
        System.out.println("SO:" + Arrays.toString(optimalChromosome.getGene_shipOrder()));
        System.out.println("SB:" + Arrays.toString(optimalChromosome.getGene_shipBerth()));
        System.out.println("SC:" + Arrays.toString(optimalChromosome.getGene_shipCrane()));
        System.out.println("平均在港时间：" + 1 / optimalChromosome.getScore());
    }

    // 1.初始化
    private void init() {
        generation = 1;

        for (int i = 0; i < POPULATION_SIZE; i++) {
            Chromosome chro = new Chromosome(SHIP_NUM, BERTH_NUM, craneMax);
            setChromosomeScore(chro);
            population.add(chro);
        }
        calculatePopulationScore();// 计算种群的适应度值
    }

    // 2.选择
    private void choose() {

        List<Chromosome> population_new = new ArrayList<>();

        while (population_new.size() < POPULATION_SIZE) {

            Chromosome chro = chooseChromosome();
            if (chro != null) {
                population_new.add(chro);
            }
        }
        population = population_new;// 新种群替代旧种群
    }

    // 3.交叉
    private void cross() {
        List<Chromosome> population_new = new ArrayList<>();

        while (population_new.size() < POPULATION_SIZE) {

            int loc1 = (int) (Math.random() * POPULATION_SIZE);
            int loc2 = (int) (Math.random() * POPULATION_SIZE);

            while (loc1 == loc2) {
                loc1 = (int) (Math.random() * POPULATION_SIZE);
                loc2 = (int) (Math.random() * POPULATION_SIZE);
            }

            Chromosome father = population.get(loc1);
            Chromosome mother = population.get(loc2);

            double rate = Math.random();
            if (rate <= CROSS_RATE) {

                List<Chromosome> children_temp = crossChromosome(father, mother);
                if (children_temp != null) {
                    population_new.addAll(children_temp);
                }
            } else {
                population_new.add(father);
                population_new.add(mother);
            }
        }
        // 用新种群替代旧种群
        population = population_new;
    }

    // 4.变异
    private void mutation() {

        for (Chromosome chro : population) {
            if (Math.random() <= MUTATION_RATE) {
                int random = (int) (Math.random() * 3); // 生成随机数，随机变异一个子染色体的基因值
                if (random == 0) {

                    // 1.变异gene_shipOrder -> 用两个位置上的数交换来模拟变异
                    int loc1 = ((int) (Math.random() * SHIP_NUM)) % SHIP_NUM;// 变异位置1
                    int loc2 = ((int) (Math.random() * SHIP_NUM)) % SHIP_NUM;// 变异位置2

                    int[] gene_shipOrder = chro.getGene_shipOrder();
                    int temp = gene_shipOrder[loc1];
                    gene_shipOrder[loc1] = gene_shipOrder[loc2];
                    gene_shipOrder[loc2] = temp;

                } else if (random == 1) {

                    // 2.变异gene_shipBerth
                    int loc = ((int) (Math.random() * SHIP_NUM)) % SHIP_NUM;// 变异的位置

                    int[] gene_shipBerth = chro.getGene_shipBerth();
                    int temp = (int) (Math.random() * BERTH_NUM + 1);
                    while (temp == gene_shipBerth[loc]) {
                        temp = (int) (Math.random() * BERTH_NUM + 1);
                    }
                    gene_shipBerth[loc] = temp;

                } else if (random == 2) {

                    // 3.变异gene_shipCrane
                    int loc = ((int) (Math.random() * SHIP_NUM)) % SHIP_NUM;

                    int[] gene_shipCrane = chro.getGene_shipCrane();
                    int temp = (int) (Math.random() * craneMax[loc] + 1);
                    while (temp == gene_shipCrane[loc]) {
                        temp = (int) (Math.random() * craneMax[loc] + 1);
                    }
                    gene_shipCrane[loc] = temp;
                }
            }
        }
    }

    // 5.计算种群适应度
    private void calculatePopulationScore() {

        // 采用和选择排序相似的方法
        bestScore = 0;
        worstScore = INF;
        double time_total = 0;
        time_average = 0;

        for (Chromosome chro : population) {

            time_total = time_total + 1 / chro.getScore();

            if (chro.getScore() > bestScore) {
                bestScore = chro.getScore();// 设置最好适应度值
            }

            if (chro.getScore() > optimalScore) {
                optimalScore = chro.getScore();// 遗传过程中的最优解
                optimalGeneraion = generation;
                optimalChromosome = chro;
            }

            if (chro.getScore() < worstScore) {
                worstScore = chro.getScore();// 设置最差适应度值
            }
        }

        time_average = time_total / POPULATION_SIZE;

        if (generation % 5 == 0) {
            bestMap.put(generation, 1 / bestScore);
            averageMap.put(generation, time_average);
            worstMap.put(generation, 1 / worstScore);
        }
    }

    // 选择准备 -> 轮盘赌算法选择
    private Chromosome chooseChromosome() {

        double totalScore = 0;
        for (Chromosome chro : population) {
            totalScore = totalScore + chro.getScore();
        }

        double[] probability = new double[POPULATION_SIZE];// 每个染色体的选择概率
        for (int i = 0; i < POPULATION_SIZE; i++) {
            probability[i] = population.get(i).getScore() / totalScore;
        }

        double slice = Math.random();
        double sum = 0;// 累积概率

        for (int i = 0; i < POPULATION_SIZE; i++) {
            sum = sum + probability[i];
            if (sum > slice) {
                return population.get(i);
            }
        }
        return null;
    }

    // 交叉准备 -> 1.复制染色体，复制一个新的对象而非对象的引用
    private Chromosome copy(Chromosome chro) {

        if (chro == null || chro.getGene_shipOrder() == null || chro.getGene_shipBerth() == null
                || chro.getGene_shipCrane() == null) {
            return null;
        }

        Chromosome copy = new Chromosome();

        int[] gene_shipOrder = chro.getGene_shipOrder();
        int[] gene_shipBerth = chro.getGene_shipBerth();
        int[] gene_shipCrane = chro.getGene_shipCrane();

        int[] order = new int[SHIP_NUM];
        int[] berth = new int[SHIP_NUM];
        int[] crane = new int[SHIP_NUM];

        for (int i = 0; i < SHIP_NUM; i++) {
            order[i] = gene_shipOrder[i];
            berth[i] = gene_shipBerth[i];
            crane[i] = gene_shipCrane[i];
        }

        copy.setGene_shipOrder(order);
        copy.setGene_shipBerth(berth);
        copy.setGene_shipCrane(crane);

        return copy;
    }

    // 交叉准备 -> 2.两点交叉
    private List<Chromosome> crossChromosome(Chromosome father, Chromosome mother) {

        if (father == null || mother == null) {
            return null;
        }
        if (father.getGene_shipOrder() == null || father.getGene_shipBerth() == null
                || father.getGene_shipCrane() == null) {
            return null;
        }
        if (mother.getGene_shipOrder() == null || mother.getGene_shipBerth() == null
                || mother.getGene_shipCrane() == null) {
            return null;
        }

        Chromosome chro1 = copy(father);
        Chromosome chro2 = copy(mother);

        // 两点交叉：从left到right的位置上发生交叉
        int a = (int) (Math.random() * SHIP_NUM);
        int b = (int) (Math.random() * SHIP_NUM);
        int left = a > b ? b : a;
        int right = a > b ? a : b;

        int[] gene_shipOrder1 = chro1.getGene_shipOrder();
        int[] gene_shipBerth1 = chro1.getGene_shipBerth();
        int[] gene_shipCrane1 = chro1.getGene_shipCrane();

        int[] gene_shipOrder2 = chro2.getGene_shipOrder();
        int[] gene_shipBerth2 = chro2.getGene_shipBerth();
        int[] gene_shipCrane2 = chro2.getGene_shipCrane();

        for (int i = left; i <= right; i++) {

            // 1.交叉gene_shipOrder[]
            int tempOrder = gene_shipOrder1[i];
            gene_shipOrder1[i] = gene_shipOrder2[i];
            gene_shipOrder2[i] = tempOrder;

            // 2.交叉gene_shipBerth[]
            int tempBerth = gene_shipBerth1[i];
            gene_shipBerth1[i] = gene_shipBerth2[i];
            gene_shipBerth2[i] = tempBerth;

            // 3.交叉gene_shipCrane
            int tempCrane = gene_shipCrane1[i];
            gene_shipCrane1[i] = gene_shipCrane2[i];
            gene_shipCrane2[i] = tempCrane;
        }

        // 判断交叉后是否符合约束条件
        // 1.靠泊顺序不能重复 -> 利用Set不重复的特性判断顺序编号是否重复
        Set<Integer> setOrder1 = new HashSet<>();
        Set<Integer> setOrder2 = new HashSet<>();
        for (int i = 0; i < SHIP_NUM; i++) {
            setOrder1.add(gene_shipOrder1[i]);
            setOrder2.add(gene_shipOrder2[i]);
        }

        // 对交叉后的染色体编码实行交换策略
        int m, n;// m,n用于定位交叉区域外的重复编号的位置
        boolean already;// 标识是否已经找到重复编号
        while (setOrder1.size() != SHIP_NUM || setOrder2.size() != SHIP_NUM) {
            m = n = -1;// m,n重新置-1
            already = false;// already重新置false

            for (int i = left; i <= right; i++) {
                for (int j = 0; j < left; j++) {
                    if (gene_shipOrder1[i] == gene_shipOrder1[j]) {
                        m = j;
                        already = true;
                        break;
                    }
                }
                if (!already) {
                    for (int j = right + 1; j < SHIP_NUM; j++) {
                        if (gene_shipOrder1[i] == gene_shipOrder1[j]) {
                            m = j;
                            already = true;
                            break;
                        }
                    }
                }
                if (already) {
                    break;
                }
            }
            already = false;// already重新置false

            for (int i = left; i <= right; i++) {
                for (int j = 0; j < left; j++) {
                    if (gene_shipOrder2[i] == gene_shipOrder2[j]) {
                        n = j;
                        already = true;
                        break;
                    }
                }
                if (!already) {
                    for (int j = right + 1; j < SHIP_NUM; j++) {
                        if (gene_shipOrder2[i] == gene_shipOrder2[j]) {
                            n = j;
                            already = true;
                            break;
                        }
                    }
                }
                if (already) {
                    break;
                }
            }

            if (m != -1 && n != -1) {
                // m和n都不等于-1时才进行交换，将m和n所在位置的SO/SB/SC都进行交换
                int temp = gene_shipOrder1[m];
                gene_shipOrder1[m] = gene_shipOrder2[n];
                gene_shipOrder2[n] = temp;

                temp = gene_shipBerth1[m];
                gene_shipBerth1[m] = gene_shipBerth2[n];
                gene_shipBerth2[n] = temp;

                temp = gene_shipCrane1[m];
                gene_shipCrane1[m] = gene_shipCrane2[n];
                gene_shipCrane2[n] = temp;
            }

            // 向集合中重新添加元素
            setOrder1.clear();
            setOrder2.clear();
            for (int i = 0; i < SHIP_NUM; i++) {
                setOrder1.add(gene_shipOrder1[i]);
                setOrder2.add(gene_shipOrder2[i]);
            }
        }

        // 2.分配的岸桥不能大于最大可接受岸桥 -> 若大于则分配岸桥改为最大可接受岸桥数
        for (int i = 0; i < SHIP_NUM; i++) {
            if (gene_shipCrane1[i] > craneMax[i]) {
                gene_shipCrane1[i] = craneMax[i];
            }
            if (gene_shipCrane2[i] > craneMax[i]) {
                gene_shipCrane2[i] = craneMax[i];
            }
        }

        List<Chromosome> listForReturn = new ArrayList<>();
        listForReturn.add(chro1);
        listForReturn.add(chro2);
        return listForReturn;
    }

    // 计算染色体的适应度值
    private void setChromosomeScore(Chromosome chro) {

        List<Integer> ships_waitBerth = new ArrayList<>();// 等待泊位的船舶集合 -> 编号，但是集合中存的是数组下标index
        List<Integer> ships_waitCrane = new ArrayList<>();// 等待岸桥的船舶集合 -> 编号， 用作 -缓冲-
        Map<Integer, Boolean> ships_service = new HashMap<>();// 服务队列的船舶集合

        int[] gene_shipOrder = chro.getGene_shipOrder();
        int[] gene_shipBerth = chro.getGene_shipBerth();
        int[] gene_shipCrane = chro.getGene_shipCrane();

        int unitTime = 0;// 单位时间
        int craneTotal = 16;// 总岸桥数16
        boolean[] berth_free = new boolean[BERTH_NUM];// 泊位空闲情况

        double[] goodsAmount = new double[SHIP_NUM];
        int[] time_arrive = loadFileToArray("time_arrive");
        int[] goodsAmount_temp = loadFileToArray("goodsAmount");

        for (int i = 0; i < SHIP_NUM; i++) {
            goodsAmount[i] = goodsAmount_temp[i] + 0.0;
        }

        int[] time_leave = new int[SHIP_NUM];// 离港时间
        for (int i = 0; i < SHIP_NUM; i++) {
            time_leave[i] = -1;
        }

        for (int i = 0; i < BERTH_NUM; i++) {
            berth_free[i] = true;
        }
        // 初始时所有船舶都在等待泊位队列
        for (int i = 0; i < gene_shipOrder.length; i++) {
            ships_waitBerth.add(i);
        }

        int order = 1;
        int k = -1;// 按顺序找到对应数组中的位置，即哪艘船
        while (((ships_waitBerth.size() != 0) || (ships_waitCrane.size() != 0) || (ships_service.size() != 0))) {

            if (ships_waitBerth.size() != 0) {
                // 找到当前靠泊顺序对应的数组下标
                for (int i = 0; i < gene_shipOrder.length; i++) {
                    if (gene_shipOrder[i] == order) {
                        k = i;
                        break;
                    }
                }

                // 若泊位空闲，且该船已经到达才让其移入等待岸桥队列
                if (berth_free[gene_shipBerth[k] - 1] && time_arrive[k] <= unitTime) {
                    // 设置泊位为忙碌
                    berth_free[gene_shipBerth[k] - 1] = false;
                    ships_waitBerth.remove((Integer) k);
                    ships_waitCrane.add(k);

                    order++;
                }
            }

            // 更新剩余装箱量 -> 当前剩余岸桥>等待岸桥集合的第一个船舶所需的岸桥数，则将等待岸桥队列的前列移入服务队列
            while (ships_waitCrane.size() > 0 && craneTotal >= gene_shipCrane[ships_waitCrane.get(0)]) {
                craneTotal = craneTotal - gene_shipCrane[ships_waitCrane.get(0)];
                ships_service.put(ships_waitCrane.remove(0), false);
            }

            for (Integer i : ships_service.keySet()) {

                // 单位时间内，剩余装箱量随时间减少，规模为分配的岸桥数 * 岸桥工作效率
                goodsAmount[i] = goodsAmount[i] - gene_shipCrane[i] * CRANE_EFFICIENCY;

                // 若剩余装箱量 <= 0，说明完成了任务，从服务队列中删除此船
                if (goodsAmount[i] <= 0) {
                    // 更新离港时间 -> 这时候还没运行到start()中的unitTime++，但此时已经过了1个单位时间
                    time_leave[i] = unitTime + 1;// 所以unitTime + 1才是真实离港时间
                    craneTotal = craneTotal + gene_shipCrane[i];// 更新总岸桥数
                    berth_free[gene_shipBerth[i] - 1] = true;// 更新空闲泊位
                    ships_service.replace(i, true);
                }
            }

            // Iterator遍历器遍历Map，删除元素
            Iterator<Entry<Integer, Boolean>> iterator = ships_service.entrySet().iterator();
            while (iterator.hasNext()) {
                Entry<Integer, Boolean> entry = iterator.next();
                if (entry.getValue()) {
                    iterator.remove();
                }
            }
            unitTime++;
        }

        double sum = 0;
        for (int i = 0; i < time_leave.length; i++) {
            // 在港时间为离港时间-到港时间
            sum = sum + (time_leave[i] - time_arrive[i]);
        }

        double score = sum / SHIP_NUM / 60;
        chro.setScore(1 / score);// 平均在港时间的倒数为适应度函数值
    }

    // 打印当前的种群目标值和其他因素
    private void printCurrentGeneration() {

        System.out.println("当前遗传代数：" + generation);
        System.out.println("最好时间：" + 1 / bestScore);
        System.out.println("最差时间：" + 1 / worstScore);
        System.out.println("平均时间：" + time_average);
        System.out.println("遗传过程中最好适应度值出现在第 " + optimalGeneraion + " 代");
        System.out.println("遗传过程中最小在港时间:" + (1 / optimalScore) + "h");
        System.out.println("-----------------------------------------");
    }

    // 加载文件
    private static int[] loadFileToArray(String str) {

        try {
            // 用缓冲流按行读取数据
            FileInputStream fis = new FileInputStream(new File("src/core/data"));
            BufferedReader br = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8));

            // 1. line -> craneMax
            String line = br.readLine();
            String[] temp1 = line.trim().split(",");

            int[] craneMax_temp = new int[temp1.length];
            for (int i = 0; i < temp1.length; i++) {
                craneMax_temp[i] = Integer.parseInt(temp1[i]);
            }

            // 2. line -> time_arrive
            line = br.readLine();
            String[] temp2 = line.trim().split(",");

            int[] time_arrive_temp = new int[temp2.length];
            for (int i = 0; i < temp2.length; i++) {
                time_arrive_temp[i] = Integer.parseInt(temp2[i]);
            }

            // 3. line -> goodsAmount
            line = br.readLine();
            String[] temp3 = line.trim().split(",");

            int[] goodsAmount_temp = new int[temp3.length];
            for (int i = 0; i < temp3.length; i++) {
                goodsAmount_temp[i] = Integer.parseInt(temp3[i]);
            }

            br.close();

            // 按照传进来的参数返回对应的数组
            if ("craneMax".equals((str))) {
                return craneMax_temp;
            } else if ("time_arrive".equals(str)) {
                return time_arrive_temp;
            } else if ("goodsAmount".equals(str)) {
                return goodsAmount_temp;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Map<Integer, Double> getBestMap() {
        return bestMap;
    }

    public Map<Integer, Double> getAverageMap() {
        return averageMap;
    }

    public Map<Integer, Double> getWorstMap() {
        return worstMap;
    }

    public int[] getOptimalChromosomeShipOrder() {
        return optimalChromosome.getGene_shipOrder();
    }

    public int[] getOptimalChromosomeShipBerth() {
        return optimalChromosome.getGene_shipBerth();
    }

    public int[] getOptimalChromosomeShipCrane() {
        return optimalChromosome.getGene_shipCrane();
    }

    public double getOptimalChromosomeScore() {
        return optimalChromosome.getScore();
    }
}

package cn.softbankrobotics.navigation2;

public class Constant {

    public static final String MAP_FILE_NAME = "map.explo";//地图数据名称
    public static final String PIC_FILE_NAME = "map.png";//地图图片名称
    public static final String FRAME_FILE_NAME = "frame.json";//标记点数据名称

    public static String shiyanshirukou ;
    public static String yibiaotai;
    public static String tongxunceshitai;
    public static String zhongyangceshitai;
    public static String bianyuanwangguanceshitai;
    public static String bofenghan;
    public static String bofenghangongzuoqu;
    public static String huiliuhan;
    public static String waikedabiaoji;
    public static String jixieshoubi;
    public static String zidongguangxuejiance;
    public static String tiepianji;
    public static String yinshuaji;
    public static String PCBjiguangdabiaoji;

    static {
        shiyanshirukou =  "实验室出口是本次测试地图的终点，实验室里面有很多东西，包括仪表台，通信测试台，中央测试台，边缘网关测试台";
        yibiaotai = "仪表台展示了各类仪器，我的程序都在这里开发";
        tongxunceshitai = "通信测试台有各类通信设备，海燕边缘网关就部署在这里";
        zhongyangceshitai = "中央测试台上面有智慧城市建设方案";
        bianyuanwangguanceshitai = "边缘网关是部署在网络边缘侧的网关，通过网络联接、协议转换等功能联接物理和数字世界，提供轻量化的联接管理、实时数据分析及应用管理功能";
        bofenghan = "波峰焊是让插件板的焊接面直接与高温液态锡接触达到焊接目的，其高温液态锡保持一个斜面，并由特殊装置使液态锡形成一道道类似波浪的现象，" +
                "所以叫\"波峰焊\"，其主要材料是焊锡条。";
        bofenghangongzuoqu = "波峰焊流程：将元件插入相应的元件孔中，预涂助焊剂，预热，波峰焊（220-240℃）冷却，切除多余插件脚，检查。";
        jixieshoubi = "机械手臂是机械人技术领域中得到最广泛实际应用的自动化机械装置，在工业制造、医学治疗、娱乐服务、军事、半导体制造以及" +
                "太空探索等领域都能见到它的身影。尽管它们的形态各有不同，但它们都有一个共同的特点，就是能够接受指令，" +
                "精确地定位到三维（或二维）空间上的某一点进行作业。";
        huiliuhan = "回流焊技术在电子制造领域并不陌生，我们电脑内使用的各种板卡上的元件都是通过这种工艺焊接到线路板上的，这种设备的内部有一个加热电路，" +
                "将空气或氮气加热到足够高的温度后吹向已经贴好元件的线路板，让元件两侧的焊料融化后与主板粘结。" +
                "这种工艺的优势是温度易于控制，焊接过程中还能避免氧化，制造成本也更容易控制。";
        waikedabiaoji = "雕刻外壳";
        zidongguangxuejiance = "自动光学检查，为高速高精度光学影像检测系统，运用机器视觉做为检测标准技术，" +
                "可以改良传统上以人力使用光学仪器进行检测的缺点，应用层面包括从高科技产业之研发、制造品管，以至国防、民生、医疗、环保、电力…等领域。";
        tiepianji = "贴片机：又称“贴装机”、“表面贴装系统”，在生产线中，它配置在点胶机或丝网印刷机之后，" +
                "是通过移动贴装头把表面贴装元器件准确地放置PCB焊盘上的一种设备。分为手动和全自动两种。";
        yinshuaji = "印刷机(The printer)是印刷文字和图像的机器。现代印刷机一般由装版、涂墨、压印、输纸（包括折叠）等机构组成。它的工作原理是：先将要印刷的文字和图像制成印版，" +
                "装在印刷机上，然后由人工或印刷机把墨涂敷于印版上有文字和图像的地方，再直接或间接地转印到纸或其他承印物（如纺织品、金属板、塑胶、皮革、木板、玻璃和陶瓷）" +
                "上，从而复制出与印版相同的印刷品。印刷机的发明和发展，对于人类文明和文化的传播具有重要作用。";
        PCBjiguangdabiaoji = "雕刻PCB板";
    }
}

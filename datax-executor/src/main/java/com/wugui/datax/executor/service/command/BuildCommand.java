package com.wugui.datax.executor.service.command;

import com.wugui.datatx.core.biz.model.TriggerParam;
import com.wugui.datatx.core.enums.IncrementTypeEnum;
import com.wugui.datatx.core.util.Constants;
import com.wugui.datatx.core.util.DateUtil;
import com.wugui.datax.executor.util.SystemUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.wugui.datatx.core.util.Constants.SPLIT_COMMA;
import static com.wugui.datax.executor.service.jobhandler.DataXConstant.*;

/**
 * DataX command build
 *
 * @author trivis 2023年1月2日03:25:18
 */
public class BuildCommand {

    /**
     * DataX command build
     *
     * @param tgParam
     * @param tmpFilePath
     * @param dataXPyPath
     * @return
     */
    public static String[] buildDataXExecutorCmd(TriggerParam tgParam, String tmpFilePath, String dataXPyPath, String pythonPath) {
        // command process
        //"--loglevel=debug"
        List<String> cmdArr = new ArrayList<>();
        cmdArr.add(pythonPath);
        String dataXHomePath = SystemUtils.getDataXHomePath();
        if (StringUtils.isNotEmpty(dataXHomePath)) {
            dataXPyPath = dataXHomePath.contains("bin") ? dataXHomePath + DEFAULT_DATAX_PY : dataXHomePath + "bin" + File.separator + DEFAULT_DATAX_PY;
        }
        cmdArr.add(dataXPyPath);
        String doc = buildJVMParam(tgParam);
        if (StringUtils.isNotBlank(doc)) {
            cmdArr.add(doc);
        }
        cmdArr.add(tmpFilePath);
        return cmdArr.toArray(new String[cmdArr.size()]);
    }

    /**
     * 构建datax运行虚拟机参数
     *
     * @param tgParam
     * @return {@link String}
     * @author trivis
     * @date 2020/9/18
     */
    private static String buildJVMParam(TriggerParam tgParam) {
        StringBuilder doc = new StringBuilder();
        String jvmParam = StringUtils.isNotBlank(tgParam.getJvmParam()) ? tgParam.getJvmParam().trim() : tgParam.getJvmParam();
        if (StringUtils.isNotBlank(jvmParam)) {
            doc.append(JVM_CM).append(TRANSFORM_QUOTES).append(jvmParam.replaceAll(SPLIT_SPACE, TRANSFORM_SPLIT_SPACE)).append(TRANSFORM_QUOTES);
        }
        return doc.toString();
    }

    /**
     * 构建datax增量参数
     *
     * @param tgParam
     * @return {@link HashMap < String, String>}
     * @author trivis
     * @date 2020/9/18
     */
    public static HashMap<String, String> buildDataXParamToMap(TriggerParam tgParam) {

        String partitionStr = tgParam.getPartitionInfo();
        Integer incrementType = tgParam.getIncrementType();
        String replaceParam = StringUtils.isNotBlank(tgParam.getReplaceParam()) ? tgParam.getReplaceParam().trim() : null;
        if (incrementType != null && replaceParam != null) {

            if (IncrementTypeEnum.TIME.getCode() == incrementType) {
                String replaceParamType = tgParam.getReplaceParamType();

                if (StringUtils.isBlank(replaceParamType) || replaceParamType.equals("Timestamp")) {
                    long startTime = tgParam.getStartTime().getTime() / 1000;
                    long endTime = tgParam.getTriggerTime().getTime() / 1000;
                    String formatParam = String.format(replaceParam, startTime, endTime);
                    return getKeyValue(formatParam);
                } else {
                    SimpleDateFormat sdf = new SimpleDateFormat(replaceParamType);
                    String endTime = sdf.format(tgParam.getTriggerTime()).replaceAll(SPLIT_SPACE, PERCENT);
                    String startTime = sdf.format(tgParam.getStartTime()).replaceAll(SPLIT_SPACE, PERCENT);
                    String formatParam = String.format(replaceParam, startTime, endTime);
                    return getKeyValue(formatParam);
                }

            }

        }

        if (incrementType != null && IncrementTypeEnum.PARTITION.getCode() == incrementType) {

            if (StringUtils.isNotBlank(partitionStr)) {
                List<String> partitionInfo = Arrays.asList(partitionStr.split(SPLIT_COMMA));
                String formatParam = String.format(PARAMS_CM_V_PT, buildPartition(partitionInfo));
                return getKeyValue(formatParam);
            }

        }

        return null;
    }

    /**
     * 任务参数封装为map
     *
     * @param formatParam
     * @return {@link HashMap< String, String>}
     * @author trivis
     * @date 2020/9/18
     */
    private static HashMap<String, String> getKeyValue(String formatParam) {
        String[] paramArr = formatParam.split(PARAMS_SYSTEM);
        HashMap<String, String> map = new HashMap<>();

        for (String param : paramArr) {
            if (StringUtils.isNotBlank(param)) {
                param = param.trim();
                String[] keyValue = param.split(PARAMS_EQUALS);
                map.put(keyValue[0], keyValue[1]);
            }
        }

        return map;
    }

    private static String buildPartition(List<String> partitionInfo) {
        String field = partitionInfo.get(0);
        int timeOffset = Integer.parseInt(partitionInfo.get(1));
        String timeFormat = partitionInfo.get(2);
        String partitionTime = DateUtil.format(DateUtil.addDays(new Date(), timeOffset), timeFormat);
        return field + Constants.EQUAL + partitionTime;
    }

}

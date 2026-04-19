package com.zfh.iot.modules.archive.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zfh.iot.modules.archive.entity.IotGroup;

import java.util.List;

public interface IotGroupService extends IService<IotGroup> {
    List<IotGroup> getGroupTree();
}

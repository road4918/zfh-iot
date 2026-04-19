package com.zfh.iot.modules.archive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zfh.iot.modules.archive.entity.IotGroup;
import com.zfh.iot.modules.archive.mapper.IotGroupMapper;
import com.zfh.iot.modules.archive.service.IotGroupService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class IotGroupServiceImpl extends ServiceImpl<IotGroupMapper, IotGroup> implements IotGroupService {

    @Override
    public List<IotGroup> getGroupTree() {
        List<IotGroup> allGroups = list();
        return buildTree(allGroups, 0L);
    }

    private List<IotGroup> buildTree(List<IotGroup> groups, Long parentId) {
        return groups.stream()
                .filter(g -> g.getParentId().equals(parentId))
                .peek(g -> g.setChildren(buildTree(groups, g.getId())))
                .collect(Collectors.toList());
    }
}

package com.pingxin403.cuckoo.user.mapper;

import com.pingxin403.cuckoo.common.mapper.DTOMapper;
import com.pingxin403.cuckoo.user.dto.UserDTO;
import com.pingxin403.cuckoo.user.entity.User;
import org.springframework.stereotype.Component;

/**
 * 用户映射器
 * 实现 DTOMapper 接口，提供 User 实体和 UserDTO 之间的转换
 */
@Component
public class UserMapper implements DTOMapper<User, UserDTO> {

    /**
     * 将 User 实体转换为 UserDTO
     * 注意：不包含密码字段，确保安全性
     * 
     * @param entity User 实体对象
     * @return UserDTO 对象，如果 entity 为 null 则返回 null
     */
    @Override
    public UserDTO toDTO(User entity) {
        if (entity == null) {
            return null;
        }
        
        return UserDTO.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .email(entity.getEmail())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * 将 UserDTO 转换为 User 实体
     * 注意：不设置 password、createdAt 和 updatedAt
     * - password 需要单独处理（加密）
     * - createdAt 和 updatedAt 由 JPA 自动管理
     * 
     * @param dto UserDTO 对象
     * @return User 实体对象，如果 dto 为 null 则返回 null
     */
    @Override
    public User toEntity(UserDTO dto) {
        if (dto == null) {
            return null;
        }
        
        return User.builder()
                .id(dto.getId())
                .username(dto.getUsername())
                .email(dto.getEmail())
                .build();
    }
    
    // toDTOList() 和 toEntityList() 方法继承自 DTOMapper 接口的默认实现
}

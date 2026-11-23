package vn.it4409.g16.auth.web.dto;

import java.util.List;
import vn.it4409.g16.auth.rbac.Role;

public record RoleResponse(String code, String name, String description, List<String> permissions) {
    public static RoleResponse of(Role r) {
        return new RoleResponse(r.getCode(), r.getName(), r.getDescription(), r.sortedPermissions());
    }
}

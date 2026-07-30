package com.bemo.hr.shared.system;

import org.springframework.data.jpa.repository.JpaRepository;

interface SystemSettingRepository extends JpaRepository<SystemSetting, String> {
}

/*
 * This file is part of LibrePlan
 *
 * Copyright (C) 2011 CafédeRed Solutions, S.L.
 * Copyright (C) 2012 Igalia, S.L.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.libreplan.business.workreports.entities;

import org.hibernate.NonUniqueResultException;
import org.libreplan.business.common.daos.IEntitySequenceDAO;
import org.libreplan.business.common.entities.EntityNameEnum;
import org.libreplan.business.common.exceptions.InstanceNotFoundException;
import org.libreplan.business.workreports.daos.IWorkReportTypeDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the default {@link WorkReportType WorkReportTypes}.<br />
 *
 * If there is no work report types, it creates a default work report type.<br />
 *
 * Even if there are already some work report types defined, it creates a work
 * report type for personal timesheets if it is not present in the database yet.
 *
 * @author Ignacio Díaz Teijido <ignacio.diaz@cafedered.com>
 * @author Manuel Rego Casasnovas <rego@igalia.com>
 */
@Component
@Scope("singleton")
public class WorkReportTypeBootstrap implements IWorkReportTypeBootstrap {

    @Autowired
    private IWorkReportTypeDAO workReportTypeDAO;

    @Autowired
    private IEntitySequenceDAO entitySequenceDAO;

    @Override
    @Transactional
    public void loadRequiredData() {
        if (workReportTypeDAO.getWorkReportTypes().size() == 0) {
            for (PredefinedWorkReportTypes predefinedWorkReportType : PredefinedWorkReportTypes
                    .values()) {
                createAndSaveWorkReportType(predefinedWorkReportType);
            }
        } else {
            createPersonalTimesheetsWorkReportTypeIfNeeded();
        }
    }

    private void createAndSaveWorkReportType(
            PredefinedWorkReportTypes predefinedWorkReportType) {
        WorkReportType workReportType = predefinedWorkReportType
                .getWorkReportType();
        workReportType.setCodeAutogenerated(true);
        workReportType
                .setCode(entitySequenceDAO
                        .getNextEntityCodeWithoutTransaction(EntityNameEnum.WORKREPORTTYPE));
        workReportTypeDAO.save(workReportType);
    }

    private void createPersonalTimesheetsWorkReportTypeIfNeeded() {
        try {
            workReportTypeDAO
                    .findUniqueByName(PredefinedWorkReportTypes.PERSONAL_TIMESHEETS
                            .getName());
        } catch (NonUniqueResultException e) {
            throw new RuntimeException(e);
        } catch (InstanceNotFoundException e) {
            createAndSaveWorkReportType(PredefinedWorkReportTypes.PERSONAL_TIMESHEETS);
        }
    }

}

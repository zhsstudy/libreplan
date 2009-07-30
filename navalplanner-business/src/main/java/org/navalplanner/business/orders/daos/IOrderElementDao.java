package org.navalplanner.business.orders.daos;
import java.util.List;

import org.navalplanner.business.common.daos.IGenericDao;
import org.navalplanner.business.common.exceptions.InstanceNotFoundException;
import org.navalplanner.business.orders.entities.OrderElement;

/**
 * Contract for {@link OrderElementDao}
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 * @author Diego Pino García <dpino@igalia.com>
 * @author Susana Montes Pedreira <smontes@wirelessgalicia.com>
 */
public interface IOrderElementDao extends IGenericDao<OrderElement, Long> {
    public OrderElement findByCode(String code);

    /**
     * Find an order element with the <code>code</code> passed as parameter
     * and which is a son of the <code>parent</code> {@link OrderElement}
     * @param parent Parent {@link OrderElement}
     * @param code code of the {@link OrderElement} to find
     * @return the {@link OrderElement} found
     */
    public OrderElement findByCode(OrderElement parent, String code);

    public List<OrderElement> findParent(
            OrderElement orderElement);

    /**
     * Returns the unique code that distinguishes an OrderElement (unique path
     * from root to OrderElement)
     *
     * @param orderElement must be attached
     * @return
     */
    public String getDistinguishedCode(OrderElement orderElement)
            throws InstanceNotFoundException;
}

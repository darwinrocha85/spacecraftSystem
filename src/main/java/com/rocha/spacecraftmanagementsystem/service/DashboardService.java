package com.rocha.spacecraftmanagementsystem.service;

import com.rocha.spacecraftmanagementsystem.exception.SpacecraftNotFoundException;
import com.rocha.spacecraftmanagementsystem.model.MuseumTicket;
import com.rocha.spacecraftmanagementsystem.model.RepairRecord;
import com.rocha.spacecraftmanagementsystem.model.Spacecraft;
import com.rocha.spacecraftmanagementsystem.model.SpacecraftStatus;
import com.rocha.spacecraftmanagementsystem.model.TheaterEvent;
import com.rocha.spacecraftmanagementsystem.model.TheaterTicket;
import com.rocha.spacecraftmanagementsystem.model.TicketStatus;
import com.rocha.spacecraftmanagementsystem.repository.MuseumTicketRepository;
import com.rocha.spacecraftmanagementsystem.repository.RepairRecordRepository;
import com.rocha.spacecraftmanagementsystem.repository.SpacecraftRepository;
import com.rocha.spacecraftmanagementsystem.repository.TheaterEventRepository;
import com.rocha.spacecraftmanagementsystem.repository.TheaterTicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Fase 6: dashboard de solo lectura para el admin (general y por nave). Compone tickets,
// eventos, naves y reparaciones que ya existen (mismo patron ad-hoc de MarketingService /
// TheaterEventService.getSales()), sin agregar ningun campo nuevo al modelo. "Ocupacion" se
// calcula siempre para HOY (no para todo el rango de un evento de teatro, que puede durar varios
// dias) - es una nocion simple y explicable para una demo. "Costs" queda fuera de esta primera
// version a proposito: RepairRecord todavia no tiene ningun campo de costo.
//
// Fix (2026-09-17): el revenue solo sumaba amountCharged, que es null en los tickets de demo de
// data.sql (son de antes de la integracion con BankIn - ver comentario ahi). Eso hacia que el
// conteo de entradas activas fuera > 0 pero el ingreso mostrado fuera $0, algo que no tiene
// sentido para el usuario. Ahora, cuando amountCharged es null, se estima con
// ticketPrice (o el fallback de BankInPaymentService) * cantidad/asientos - mismo calculo que ya
// hace el servicio de compra antes de cobrar.
@Service
public class DashboardService {

    @Autowired
    private SpacecraftRepository spacecraftRepository;

    @Autowired
    private MuseumTicketRepository museumTicketRepository;

    @Autowired
    private TheaterTicketRepository theaterTicketRepository;

    @Autowired
    private TheaterEventRepository theaterEventRepository;

    @Autowired
    private RepairRecordRepository repairRecordRepository;

    public Map<String, Object> getOverview() {
        List<Spacecraft> allSpacecrafts = spacecraftRepository.findAll();
        List<MuseumTicket> allMuseumTickets = museumTicketRepository.findAll();
        List<TheaterTicket> allTheaterTickets = theaterTicketRepository.findAll();
        List<TheaterEvent> allEvents = theaterEventRepository.findAll();
        Map<Long, TheaterEvent> eventsById = indexEventsById(allEvents);
        Map<Long, Spacecraft> spacecraftById = indexSpacecraftsById(allSpacecrafts);

        Map<String, Object> result = new HashMap<>();
        result.put("fleet", buildFleetSummary(allSpacecrafts));
        result.put("revenue",
                computeRevenue(allMuseumTickets, allTheaterTickets, eventsById, spacecraftById, null).asMap());
        result.put("tickets", computeTicketCounts(allMuseumTickets, allTheaterTickets, eventsById, null).asMap());
        result.put("occupancy", computeOccupancyToday(allSpacecrafts, allMuseumTickets, allTheaterTickets, allEvents, null));
        result.put("topSpacecraftsByRevenue",
                topSpacecraftsByRevenue(allSpacecrafts, allMuseumTickets, allTheaterTickets, eventsById, spacecraftById));
        return result;
    }

    public Map<String, Object> getSpacecraftDashboard(Long spacecraftId) {
        Spacecraft spacecraft = spacecraftRepository.findById(spacecraftId)
                .orElseThrow(() -> new SpacecraftNotFoundException("Spacecraft with ID " + spacecraftId + " not found"));

        List<Spacecraft> allSpacecrafts = spacecraftRepository.findAll();
        List<MuseumTicket> allMuseumTickets = museumTicketRepository.findAll();
        List<TheaterTicket> allTheaterTickets = theaterTicketRepository.findAll();
        List<TheaterEvent> allEvents = theaterEventRepository.findAll();
        Map<Long, TheaterEvent> eventsById = indexEventsById(allEvents);
        Map<Long, Spacecraft> spacecraftById = indexSpacecraftsById(allSpacecrafts);

        List<RepairRecord> repairs = repairRecordRepository.findBySpacecraftIdOrderBySentAtDesc(spacecraftId);

        Map<String, Object> repairInfo = new HashMap<>();
        repairInfo.put("totalVisits", repairs.size());
        repairInfo.put("currentlyInRepair", !isOperativa(spacecraft));

        Map<String, Object> result = new HashMap<>();
        result.put("spacecraftId", spacecraft.getId());
        result.put("name", spacecraft.getName());
        result.put("franchise", spacecraft.getFranchise());
        result.put("status", spacecraft.getStatus());
        result.put("isMuseum", Boolean.TRUE.equals(spacecraft.getIsMuseum()));
        result.put("isTheater", Boolean.TRUE.equals(spacecraft.getIsTheater()));
        result.put("revenue",
                computeRevenue(allMuseumTickets, allTheaterTickets, eventsById, spacecraftById, spacecraftId).asMap());
        result.put("tickets", computeTicketCounts(allMuseumTickets, allTheaterTickets, eventsById, spacecraftId).asMap());
        result.put("occupancy",
                computeOccupancyToday(allSpacecrafts, allMuseumTickets, allTheaterTickets, allEvents, spacecraftId));
        result.put("repairs", repairInfo);
        return result;
    }

    // Detalle de entradas activas (museo + teatro), con fecha/hora, comprador y el desglose de
    // costo. spacecraftIdFilter == null trae toda la flota (dashboard general); si viene con un
    // id, solo esa nave (modal por nave). Se ordena por fecha/hora de la visita o funcion, la mas
    // proxima primero.
    public List<Map<String, Object>> getTicketDetails(Long spacecraftIdFilter) {
        List<Spacecraft> allSpacecrafts = spacecraftRepository.findAll();
        List<MuseumTicket> allMuseumTickets = museumTicketRepository.findAll();
        List<TheaterTicket> allTheaterTickets = theaterTicketRepository.findAll();
        List<TheaterEvent> allEvents = theaterEventRepository.findAll();
        Map<Long, TheaterEvent> eventsById = indexEventsById(allEvents);
        Map<Long, Spacecraft> spacecraftById = indexSpacecraftsById(allSpacecrafts);

        List<Map<String, Object>> details = new ArrayList<>();

        for (MuseumTicket t : allMuseumTickets) {
            if (t.getStatus() != TicketStatus.ACTIVE) {
                continue;
            }
            if (spacecraftIdFilter != null && !spacecraftIdFilter.equals(t.getSpacecraftId())) {
                continue;
            }
            Spacecraft s = spacecraftById.get(t.getSpacecraftId());
            int quantity = t.getQuantity() == null ? 0 : t.getQuantity();
            double unitPrice = resolveUnitPrice(s);
            double total = effectiveAmount(t.getAmountCharged(), unitPrice, quantity);

            Map<String, Object> item = new HashMap<>();
            item.put("type", "museum");
            item.put("spacecraftId", t.getSpacecraftId());
            item.put("spacecraftName", s != null ? s.getName() : "—");
            item.put("date", t.getVisitDate());
            item.put("time", t.getVisitTime());
            item.put("quantity", quantity);
            item.put("buyerName", t.getBuyerName());
            item.put("buyerEmail", t.getBuyerEmail());
            item.put("confirmationCode", t.getConfirmationCode());
            item.put("unitPrice", round2(unitPrice));
            item.put("totalPrice", round2(total));
            details.add(item);
        }

        for (TheaterTicket t : allTheaterTickets) {
            if (t.getStatus() != TicketStatus.ACTIVE) {
                continue;
            }
            TheaterEvent event = eventsById.get(t.getEventId());
            if (event == null) {
                continue;
            }
            if (spacecraftIdFilter != null && !spacecraftIdFilter.equals(event.getSpacecraftId())) {
                continue;
            }
            Spacecraft s = spacecraftById.get(event.getSpacecraftId());
            int quantity = t.getSeats() == null ? 0 : t.getSeats().size();
            double unitPrice = resolveUnitPrice(s);
            double total = effectiveAmount(t.getAmountCharged(), unitPrice, quantity);

            Map<String, Object> item = new HashMap<>();
            item.put("type", "theater");
            item.put("spacecraftId", event.getSpacecraftId());
            item.put("spacecraftName", s != null ? s.getName() : "—");
            item.put("date", t.getFunctionDate());
            item.put("time", event.getTime());
            item.put("quantity", quantity);
            item.put("buyerName", t.getBuyerName());
            item.put("buyerEmail", t.getBuyerEmail());
            item.put("confirmationCode", t.getConfirmationCode());
            item.put("unitPrice", round2(unitPrice));
            item.put("totalPrice", round2(total));
            details.add(item);
        }

        details.sort(Comparator
                .comparing((Map<String, Object> m) -> (LocalDate) m.get("date"), Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(m -> (LocalTime) m.get("time"), Comparator.nullsLast(Comparator.naturalOrder())));

        return details;
    }

    private Map<Long, TheaterEvent> indexEventsById(List<TheaterEvent> events) {
        Map<Long, TheaterEvent> map = new HashMap<>();
        for (TheaterEvent e : events) {
            map.put(e.getId(), e);
        }
        return map;
    }

    private Map<Long, Spacecraft> indexSpacecraftsById(List<Spacecraft> spacecrafts) {
        Map<Long, Spacecraft> map = new HashMap<>();
        for (Spacecraft s : spacecrafts) {
            map.put(s.getId(), s);
        }
        return map;
    }

    private Map<String, Object> buildFleetSummary(List<Spacecraft> allSpacecrafts) {
        Map<String, Integer> byStatus = new LinkedHashMap<>();
        for (SpacecraftStatus s : SpacecraftStatus.values()) {
            byStatus.put(s.name(), 0);
        }
        for (Spacecraft s : allSpacecrafts) {
            SpacecraftStatus status = s.getStatus() != null ? s.getStatus() : SpacecraftStatus.OPERATIVA;
            byStatus.merge(status.name(), 1, Integer::sum);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("total", allSpacecrafts.size());
        result.put("byStatus", byStatus);
        return result;
    }

    private RevenueTotals computeRevenue(List<MuseumTicket> museumTickets, List<TheaterTicket> theaterTickets,
                                          Map<Long, TheaterEvent> eventsById, Map<Long, Spacecraft> spacecraftById,
                                          Long spacecraftIdFilter) {
        double museumRevenue = 0;
        for (MuseumTicket t : museumTickets) {
            if (spacecraftIdFilter != null && !spacecraftIdFilter.equals(t.getSpacecraftId())) {
                continue;
            }
            if (t.getStatus() != TicketStatus.ACTIVE) {
                continue;
            }
            double unitPrice = resolveUnitPrice(spacecraftById.get(t.getSpacecraftId()));
            int quantity = t.getQuantity() == null ? 0 : t.getQuantity();
            museumRevenue += effectiveAmount(t.getAmountCharged(), unitPrice, quantity);
        }
        double theaterRevenue = 0;
        for (TheaterTicket t : theaterTickets) {
            TheaterEvent event = eventsById.get(t.getEventId());
            if (event == null) {
                continue;
            }
            if (spacecraftIdFilter != null && !spacecraftIdFilter.equals(event.getSpacecraftId())) {
                continue;
            }
            if (t.getStatus() != TicketStatus.ACTIVE) {
                continue;
            }
            double unitPrice = resolveUnitPrice(spacecraftById.get(event.getSpacecraftId()));
            int quantity = t.getSeats() == null ? 0 : t.getSeats().size();
            theaterRevenue += effectiveAmount(t.getAmountCharged(), unitPrice, quantity);
        }
        return new RevenueTotals(museumRevenue, theaterRevenue);
    }

    private TicketCounts computeTicketCounts(List<MuseumTicket> museumTickets, List<TheaterTicket> theaterTickets,
                                              Map<Long, TheaterEvent> eventsById, Long spacecraftIdFilter) {
        int activeMuseum = 0;
        int cancelledMuseum = 0;
        int activeTheater = 0;
        int cancelledTheater = 0;

        for (MuseumTicket t : museumTickets) {
            if (spacecraftIdFilter != null && !spacecraftIdFilter.equals(t.getSpacecraftId())) {
                continue;
            }
            if (t.getStatus() == TicketStatus.ACTIVE) {
                activeMuseum++;
            } else if (t.getStatus() == TicketStatus.CANCELLED) {
                cancelledMuseum++;
            }
        }
        for (TheaterTicket t : theaterTickets) {
            TheaterEvent event = eventsById.get(t.getEventId());
            if (event == null) {
                continue;
            }
            if (spacecraftIdFilter != null && !spacecraftIdFilter.equals(event.getSpacecraftId())) {
                continue;
            }
            if (t.getStatus() == TicketStatus.ACTIVE) {
                activeTheater++;
            } else if (t.getStatus() == TicketStatus.CANCELLED) {
                cancelledTheater++;
            }
        }
        return new TicketCounts(activeMuseum, cancelledMuseum, activeTheater, cancelledTheater);
    }

    // Ocupacion de HOY: cuanto de la capacidad de museo/teatro reservable hoy ya esta vendido.
    // No intenta calcular la ocupacion de todo el rango de un evento de teatro (que puede durar
    // varios dias) - solo la funcion/turno de hoy, que es una nocion simple y facil de explicar.
    private Map<String, Object> computeOccupancyToday(List<Spacecraft> allSpacecrafts, List<MuseumTicket> museumTickets,
                                                        List<TheaterTicket> theaterTickets, List<TheaterEvent> allEvents,
                                                        Long spacecraftIdFilter) {
        LocalDate today = LocalDate.now();
        Map<Long, Spacecraft> spacecraftById = new HashMap<>();
        for (Spacecraft s : allSpacecrafts) {
            spacecraftById.put(s.getId(), s);
        }

        int museumCapacityToday = 0;
        for (Spacecraft s : allSpacecrafts) {
            if (spacecraftIdFilter != null && !spacecraftIdFilter.equals(s.getId())) {
                continue;
            }
            if (!Boolean.TRUE.equals(s.getIsMuseum()) || !isOperativa(s)) {
                continue;
            }
            if (s.getMuseumCapacity() != null) {
                museumCapacityToday += s.getMuseumCapacity();
            }
        }
        int museumReservedToday = 0;
        for (MuseumTicket t : museumTickets) {
            if (spacecraftIdFilter != null && !spacecraftIdFilter.equals(t.getSpacecraftId())) {
                continue;
            }
            if (t.getStatus() == TicketStatus.ACTIVE && today.equals(t.getVisitDate()) && t.getQuantity() != null) {
                museumReservedToday += t.getQuantity();
            }
        }

        int eventsToday = 0;
        Set<Long> eventIdsToday = new HashSet<>();
        for (TheaterEvent e : allEvents) {
            if (spacecraftIdFilter != null && !spacecraftIdFilter.equals(e.getSpacecraftId())) {
                continue;
            }
            Spacecraft s = spacecraftById.get(e.getSpacecraftId());
            if (s == null || !isOperativa(s)) {
                continue;
            }
            if (e.getStartDate() == null || e.getEndDate() == null) {
                continue;
            }
            if (!today.isBefore(e.getStartDate()) && !today.isAfter(e.getEndDate())) {
                eventsToday++;
                eventIdsToday.add(e.getId());
            }
        }
        int theaterSeatsSoldToday = 0;
        for (TheaterTicket t : theaterTickets) {
            if (t.getStatus() == TicketStatus.ACTIVE && today.equals(t.getFunctionDate())
                    && eventIdsToday.contains(t.getEventId())) {
                theaterSeatsSoldToday += t.getSeats() == null ? 0 : t.getSeats().size();
            }
        }
        int theaterSeatsAvailableToday = eventsToday * TheaterEventService.SEATS_PER_FUNCTION;

        Map<String, Object> result = new HashMap<>();
        result.put("museumCapacityToday", museumCapacityToday);
        result.put("museumReservedToday", museumReservedToday);
        result.put("museumPercent",
                museumCapacityToday > 0 ? round1(museumReservedToday * 100.0 / museumCapacityToday) : null);
        result.put("eventsToday", eventsToday);
        result.put("theaterSeatsAvailableToday", theaterSeatsAvailableToday);
        result.put("theaterSeatsSoldToday", theaterSeatsSoldToday);
        result.put("theaterPercent",
                theaterSeatsAvailableToday > 0 ? round1(theaterSeatsSoldToday * 100.0 / theaterSeatsAvailableToday) : null);
        return result;
    }

    private List<Map<String, Object>> topSpacecraftsByRevenue(List<Spacecraft> allSpacecrafts,
                                                                List<MuseumTicket> museumTickets,
                                                                List<TheaterTicket> theaterTickets,
                                                                Map<Long, TheaterEvent> eventsById,
                                                                Map<Long, Spacecraft> spacecraftById) {
        Map<Long, Double> revenueByShip = new HashMap<>();
        for (MuseumTicket t : museumTickets) {
            if (t.getStatus() != TicketStatus.ACTIVE) {
                continue;
            }
            double unitPrice = resolveUnitPrice(spacecraftById.get(t.getSpacecraftId()));
            int quantity = t.getQuantity() == null ? 0 : t.getQuantity();
            revenueByShip.merge(t.getSpacecraftId(), effectiveAmount(t.getAmountCharged(), unitPrice, quantity), Double::sum);
        }
        for (TheaterTicket t : theaterTickets) {
            TheaterEvent event = eventsById.get(t.getEventId());
            if (event == null) {
                continue;
            }
            if (t.getStatus() != TicketStatus.ACTIVE) {
                continue;
            }
            double unitPrice = resolveUnitPrice(spacecraftById.get(event.getSpacecraftId()));
            int quantity = t.getSeats() == null ? 0 : t.getSeats().size();
            revenueByShip.merge(event.getSpacecraftId(), effectiveAmount(t.getAmountCharged(), unitPrice, quantity), Double::sum);
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<Long, Double> entry : revenueByShip.entrySet()) {
            Spacecraft s = spacecraftById.get(entry.getKey());
            if (s == null) {
                continue;
            }
            Map<String, Object> item = new HashMap<>();
            item.put("spacecraftId", s.getId());
            item.put("name", s.getName());
            item.put("franchise", s.getFranchise());
            item.put("revenue", round2(entry.getValue()));
            list.add(item);
        }
        list.sort((a, b) -> Double.compare((Double) b.get("revenue"), (Double) a.get("revenue")));
        return list.size() > 5 ? list.subList(0, 5) : list;
    }

    // Precio de referencia de una nave: su ticketPrice si tiene, si no el fallback general de
    // BankIn - mismo criterio que ya usan MuseumTicketService/TheaterTicketService al cobrar.
    private double resolveUnitPrice(Spacecraft spacecraft) {
        if (spacecraft != null && spacecraft.getTicketPrice() != null) {
            return spacecraft.getTicketPrice();
        }
        return BankInPaymentService.DEFAULT_TICKET_PRICE;
    }

    // Si el ticket ya tiene un cobro real registrado (amountCharged, fase de integracion con
    // BankIn), se usa ese monto tal cual. Si no (tickets de demo/seed anteriores a esa fase, sin
    // amountCharged), se estima con precio unitario * cantidad para que el dashboard no muestre
    // $0 de ingresos con entradas activas de por medio.
    private double effectiveAmount(Double amountCharged, double unitPrice, int quantity) {
        if (amountCharged != null) {
            return amountCharged;
        }
        return unitPrice * quantity;
    }

    private boolean isOperativa(Spacecraft s) {
        return s.getStatus() == null || s.getStatus() == SpacecraftStatus.OPERATIVA;
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record RevenueTotals(double museum, double theater) {
        Map<String, Object> asMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("total", round2(museum + theater));
            map.put("museum", round2(museum));
            map.put("theater", round2(theater));
            return map;
        }
    }

    private record TicketCounts(int activeMuseum, int cancelledMuseum, int activeTheater, int cancelledTheater) {
        Map<String, Object> asMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("activeMuseum", activeMuseum);
            map.put("cancelledMuseum", cancelledMuseum);
            map.put("activeTheater", activeTheater);
            map.put("cancelledTheater", cancelledTheater);
            return map;
        }
    }
}

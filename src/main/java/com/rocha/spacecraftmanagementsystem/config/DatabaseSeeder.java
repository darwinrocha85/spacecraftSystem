package com.rocha.spacecraftmanagementsystem.config;

import com.rocha.spacecraftmanagementsystem.model.MuseumSchedule;
import com.rocha.spacecraftmanagementsystem.model.MuseumTicket;
import com.rocha.spacecraftmanagementsystem.model.Spacecraft;
import com.rocha.spacecraftmanagementsystem.model.SpacecraftStatus;
import com.rocha.spacecraftmanagementsystem.model.TheaterEvent;
import com.rocha.spacecraftmanagementsystem.model.TheaterEventType;
import com.rocha.spacecraftmanagementsystem.model.TheaterTicket;
import com.rocha.spacecraftmanagementsystem.model.TicketStatus;
import com.rocha.spacecraftmanagementsystem.repository.MuseumScheduleRepository;
import com.rocha.spacecraftmanagementsystem.repository.MuseumTicketRepository;
import com.rocha.spacecraftmanagementsystem.repository.SpacecraftRepository;
import com.rocha.spacecraftmanagementsystem.repository.TheaterEventRepository;
import com.rocha.spacecraftmanagementsystem.repository.TheaterTicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);

    private final SpacecraftRepository spacecraftRepository;
    private final MuseumScheduleRepository museumScheduleRepository;
    private final TheaterEventRepository theaterEventRepository;
    private final MuseumTicketRepository museumTicketRepository;
    private final TheaterTicketRepository theaterTicketRepository;

    public DatabaseSeeder(SpacecraftRepository spacecraftRepository,
                          MuseumScheduleRepository museumScheduleRepository,
                          TheaterEventRepository theaterEventRepository,
                          MuseumTicketRepository museumTicketRepository,
                          TheaterTicketRepository theaterTicketRepository) {
        this.spacecraftRepository = spacecraftRepository;
        this.museumScheduleRepository = museumScheduleRepository;
        this.theaterEventRepository = theaterEventRepository;
        this.museumTicketRepository = museumTicketRepository;
        this.theaterTicketRepository = theaterTicketRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (spacecraftRepository.count() > 0) {
            log.info("DatabaseSeeder: DB ya tiene datos ({} naves), skip seed", spacecraftRepository.count());
            return;
        }
        log.info("DatabaseSeeder: DB vacia, sembrando datos demo (SQLite file)");

        Spacecraft s1 = new Spacecraft();
        s1.setName("USS Enterprise");
        s1.setFranchise("Star Trek");
        s1.setCrewCapacity(430);
        s1.setSpeed(9.6);
        s1.setSpacecraftType("Exploration");
        s1.setIsArmed(true);
        s1.setIsMuseum(true);
        s1.setMuseumCapacity(150);
        s1.setIsTheater(false);
        s1.setStatus(SpacecraftStatus.OPERATIVA);
        s1.setTicketPrice(12.50);
        s1 = spacecraftRepository.save(s1);

        Spacecraft s2 = new Spacecraft();
        s2.setName("Millennium Falcon");
        s2.setFranchise("Star Wars");
        s2.setCrewCapacity(6);
        s2.setSpeed(1050.0);
        s2.setSpacecraftType("Freighter");
        s2.setIsArmed(true);
        s2.setIsMuseum(false);
        s2.setIsTheater(true);
        s2.setStatus(SpacecraftStatus.OPERATIVA);
        s2.setTicketPrice(35.00);
        s2 = spacecraftRepository.save(s2);

        Spacecraft s3 = new Spacecraft();
        s3.setName("Galactica");
        s3.setFranchise("Battlestar Galactica");
        s3.setCrewCapacity(2500);
        s3.setSpeed(8.0);
        s3.setSpacecraftType("Battlestar");
        s3.setIsArmed(true);
        s3.setIsMuseum(true);
        s3.setMuseumCapacity(300);
        s3.setIsTheater(true);
        s3.setStatus(SpacecraftStatus.OPERATIVA);
        s3.setTicketPrice(20.00);
        s3 = spacecraftRepository.save(s3);

        Spacecraft s4 = new Spacecraft();
        s4.setName("Serenity");
        s4.setFranchise("Firefly");
        s4.setCrewCapacity(5);
        s4.setSpeed(1.5);
        s4.setSpacecraftType("Transport");
        s4.setIsArmed(false);
        s4.setStatus(SpacecraftStatus.OPERATIVA);
        s4 = spacecraftRepository.save(s4);

        LocalDate today = LocalDate.now();
        for (int i = 0; i < 8; i++) {
            LocalDate d = today.plusDays(i);
            MuseumSchedule ms1 = new MuseumSchedule();
            ms1.setSpacecraftId(s1.getId());
            ms1.setDate(d);
            ms1.setOpenTime(LocalTime.of(9, 0));
            ms1.setCloseTime(LocalTime.of(18, 0));
            museumScheduleRepository.save(ms1);

            MuseumSchedule ms3 = new MuseumSchedule();
            ms3.setSpacecraftId(s3.getId());
            ms3.setDate(d);
            ms3.setOpenTime(LocalTime.of(10, 0));
            ms3.setCloseTime(LocalTime.of(19, 0));
            museumScheduleRepository.save(ms3);
        }

        TheaterEvent e1 = new TheaterEvent();
        e1.setSpacecraftId(s2.getId());
        e1.setEventType(TheaterEventType.MUSICA);
        e1.setStartDate(today);
        e1.setEndDate(today.plusDays(7));
        e1.setTime(LocalTime.of(19, 0));
        e1 = theaterEventRepository.save(e1);

        TheaterEvent e2 = new TheaterEvent();
        e2.setSpacecraftId(s2.getId());
        e2.setEventType(TheaterEventType.ARTES);
        e2.setStartDate(today);
        e2.setEndDate(today.plusDays(4));
        e2.setTime(LocalTime.of(21, 0));
        e2 = theaterEventRepository.save(e2);

        TheaterEvent e3 = new TheaterEvent();
        e3.setSpacecraftId(s3.getId());
        e3.setEventType(TheaterEventType.LIBRE);
        e3.setStartDate(today);
        e3.setEndDate(today.plusDays(7));
        e3.setTime(LocalTime.of(18, 0));
        e3 = theaterEventRepository.save(e3);

        saveMuseumTicket(s1.getId(), today, LocalTime.of(10, 0), 3, "Alice Nova", "alice.nova@example.com", "MUS-DEMO0001", TicketStatus.ACTIVE);
        saveMuseumTicket(s1.getId(), today, LocalTime.of(14, 0), 2, "Ben Orbit", "ben.orbit@example.com", "MUS-DEMO0002", TicketStatus.ACTIVE);
        saveMuseumTicket(s1.getId(), today.plusDays(1), LocalTime.of(11, 0), 5, "Cleo Star", "cleo.star@example.com", "MUS-DEMO0003", TicketStatus.ACTIVE);
        saveMuseumTicket(s3.getId(), today, LocalTime.of(12, 0), 8, "Dax Comet", "dax.comet@example.com", "MUS-DEMO0004", TicketStatus.ACTIVE);
        saveMuseumTicket(s3.getId(), today.plusDays(2), LocalTime.of(15, 0), 4, "Eve Pulsar", "eve.pulsar@example.com", "MUS-DEMO0005", TicketStatus.ACTIVE);
        saveMuseumTicket(s3.getId(), today, LocalTime.of(17, 0), 6, "Finn Quasar", "finn.quasar@example.com", "MUS-DEMO0006", TicketStatus.CANCELLED);

        saveTheaterTicket(e1.getId(), today, "Alice Nova", "alice.nova@example.com", "THT-DEMO0001", TicketStatus.ACTIVE, List.of(5, 6, 7));
        saveTheaterTicket(e1.getId(), today.plusDays(1), "Ben Orbit", "ben.orbit@example.com", "THT-DEMO0002", TicketStatus.ACTIVE, List.of(10, 11));
        saveTheaterTicket(e3.getId(), today, "Cleo Star", "cleo.star@example.com", "THT-DEMO0003", TicketStatus.ACTIVE, List.of(1, 2, 3, 4));
        saveTheaterTicket(e3.getId(), today, "Dax Comet", "dax.comet@example.com", "THT-DEMO0004", TicketStatus.CANCELLED, List.of(50));

        log.info("DatabaseSeeder: seed completado");
    }

    private void saveMuseumTicket(Long spacecraftId, LocalDate visitDate, LocalTime visitTime, int quantity,
                                  String buyerName, String buyerEmail, String code, TicketStatus status) {
        MuseumTicket t = new MuseumTicket();
        t.setSpacecraftId(spacecraftId);
        t.setVisitDate(visitDate);
        t.setVisitTime(visitTime);
        t.setQuantity(quantity);
        t.setBuyerName(buyerName);
        t.setBuyerEmail(buyerEmail);
        t.setConfirmationCode(code);
        t.setStatus(status);
        museumTicketRepository.save(t);
    }

    private void saveTheaterTicket(Long eventId, LocalDate functionDate, String buyerName, String buyerEmail,
                                   String code, TicketStatus status, List<Integer> seats) {
        TheaterTicket t = new TheaterTicket();
        t.setEventId(eventId);
        t.setFunctionDate(functionDate);
        t.setBuyerName(buyerName);
        t.setBuyerEmail(buyerEmail);
        t.setConfirmationCode(code);
        t.setStatus(status);
        t.setSeats(seats);
        theaterTicketRepository.save(t);
    }
}

INSERT INTO spacecrafts (name, franchise, crew_capacity, speed, spacecraft_type, is_armed, status, ticket_price) VALUES
('USS Enterprise', 'Star Trek', 430, 9.6, 'Exploration', true, 'OPERATIVA', 12.50),
('Millennium Falcon', 'Star Wars', 6, 1050, 'Freighter', true, 'OPERATIVA', 35.00),
('Galactica', 'Battlestar Galactica', 2500, 8.0, 'Battlestar', true, 'OPERATIVA', 20.00),
('Serenity', 'Firefly', 5, 1.5, 'Transport', false, 'OPERATIVA', NULL);

-- Demo simplificada a proposito: 4 naves, una por cada combinacion posible de recinto,
-- para no depender de cual nave se pruebe -> todas tienen datos de sobra.
--   1: USS Enterprise    -> solo museo   ($12.50 por persona)
--   2: Millennium Falcon -> solo teatro  ($35.00 por asiento)
--   3: Galactica         -> museo + teatro ($20.00 por persona/asiento)
--   4: Serenity          -> ninguno (nave "normal", sin recinto ni precio)
UPDATE spacecrafts SET is_museum = true, museum_capacity = 150 WHERE id = 1;
UPDATE spacecrafts SET is_theater = true WHERE id = 2;
UPDATE spacecrafts SET is_museum = true, is_theater = true, museum_capacity = 300 WHERE id = 3;
-- id 4 (Serenity) se deja sin recinto: is_museum / is_theater quedan null

-- Horarios de museo (naves 1 y 3): los 8 dias completos de la ventana movil (hoy + 7),
-- para que la nave este abierta sin importar que dia de esa ventana se pruebe.
INSERT INTO museum_schedules (spacecraft_id, schedule_date, open_time, close_time) VALUES
(1, CURRENT_DATE, '09:00:00', '18:00:00'),
(1, DATEADD('DAY', 1, CURRENT_DATE), '09:00:00', '18:00:00'),
(1, DATEADD('DAY', 2, CURRENT_DATE), '09:00:00', '18:00:00'),
(1, DATEADD('DAY', 3, CURRENT_DATE), '09:00:00', '18:00:00'),
(1, DATEADD('DAY', 4, CURRENT_DATE), '09:00:00', '18:00:00'),
(1, DATEADD('DAY', 5, CURRENT_DATE), '09:00:00', '18:00:00'),
(1, DATEADD('DAY', 6, CURRENT_DATE), '09:00:00', '18:00:00'),
(1, DATEADD('DAY', 7, CURRENT_DATE), '09:00:00', '18:00:00'),
(3, CURRENT_DATE, '10:00:00', '19:00:00'),
(3, DATEADD('DAY', 1, CURRENT_DATE), '10:00:00', '19:00:00'),
(3, DATEADD('DAY', 2, CURRENT_DATE), '10:00:00', '19:00:00'),
(3, DATEADD('DAY', 3, CURRENT_DATE), '10:00:00', '19:00:00'),
(3, DATEADD('DAY', 4, CURRENT_DATE), '10:00:00', '19:00:00'),
(3, DATEADD('DAY', 5, CURRENT_DATE), '10:00:00', '19:00:00'),
(3, DATEADD('DAY', 6, CURRENT_DATE), '10:00:00', '19:00:00'),
(3, DATEADD('DAY', 7, CURRENT_DATE), '10:00:00', '19:00:00');

-- Funciones de teatro (naves 2 y 3): un par de eventos por nave con fechas relativas
-- a CURRENT_DATE, para que la app de entradas y el panel de ventas siempre tengan algo.
-- ids resultantes (nave nueva, DB en memoria): 1 y 2 -> Millennium Falcon, 3 -> Galactica
INSERT INTO theater_events (spacecraft_id, event_type, start_date, end_date, event_time) VALUES
(2, 'MUSICA', CURRENT_DATE, DATEADD('DAY', 7, CURRENT_DATE), '19:00:00'),
(2, 'ARTES', CURRENT_DATE, DATEADD('DAY', 4, CURRENT_DATE), '21:00:00'),
(3, 'LIBRE', CURRENT_DATE, DATEADD('DAY', 7, CURRENT_DATE), '18:00:00');

-- Entradas de museo ya vendidas (naves 1 y 3), en horas dentro del horario de arriba,
-- para que el panel de "entradas vendidas" del admin no arranque en cero.
-- (De antes de la fase de integracion con BankIn: sin bankin_transaction_id/amount_charged.)
INSERT INTO museum_tickets (spacecraft_id, visit_date, visit_time, quantity, buyer_name, buyer_email, confirmation_code, status) VALUES
(1, CURRENT_DATE, '10:00:00', 3, 'Alice Nova', 'alice.nova@example.com', 'MUS-DEMO0001', 'ACTIVE'),
(1, CURRENT_DATE, '14:00:00', 2, 'Ben Orbit', 'ben.orbit@example.com', 'MUS-DEMO0002', 'ACTIVE'),
(1, DATEADD('DAY', 1, CURRENT_DATE), '11:00:00', 5, 'Cleo Star', 'cleo.star@example.com', 'MUS-DEMO0003', 'ACTIVE'),
(3, CURRENT_DATE, '12:00:00', 8, 'Dax Comet', 'dax.comet@example.com', 'MUS-DEMO0004', 'ACTIVE'),
(3, DATEADD('DAY', 2, CURRENT_DATE), '15:00:00', 4, 'Eve Pulsar', 'eve.pulsar@example.com', 'MUS-DEMO0005', 'ACTIVE'),
(3, CURRENT_DATE, '17:00:00', 6, 'Finn Quasar', 'finn.quasar@example.com', 'MUS-DEMO0006', 'CANCELLED');

-- Entradas de teatro ya vendidas (eventos 1 y 3), con asientos ya ocupados para que el
-- mapa de 100 asientos y el resumen de ventas por evento tengan datos reales.
-- ids resultantes: entradas 1-4, en ese orden
INSERT INTO theater_tickets (event_id, function_date, buyer_name, buyer_email, confirmation_code, status) VALUES
(1, CURRENT_DATE, 'Alice Nova', 'alice.nova@example.com', 'THT-DEMO0001', 'ACTIVE'),
(1, DATEADD('DAY', 1, CURRENT_DATE), 'Ben Orbit', 'ben.orbit@example.com', 'THT-DEMO0002', 'ACTIVE'),
(3, CURRENT_DATE, 'Cleo Star', 'cleo.star@example.com', 'THT-DEMO0003', 'ACTIVE'),
(3, CURRENT_DATE, 'Dax Comet', 'dax.comet@example.com', 'THT-DEMO0004', 'CANCELLED');

INSERT INTO theater_ticket_seats (ticket_id, seat_number) VALUES
(1, 5), (1, 6), (1, 7),
(2, 10), (2, 11),
(3, 1), (3, 2), (3, 3), (3, 4),
(4, 50);

INSERT INTO spacecrafts (name, franchise, crew_capacity, speed, spacecraft_type, is_armed) VALUES
('USS Enterprise', 'Star Trek', 430, 9.6, 'Exploration', true),
('Millennium Falcon', 'Star Wars', 6, 1050, 'Freighter', true),
('X-Wing', 'Star Wars', 1, 1050, 'Starfighter', true),
('Y-Wing', 'Star Wars', 2, 950, 'Starfighter', true),
('Galactica', 'Battlestar Galactica', 2500, 8.0, 'Battlestar', true),
('Serenity', 'Firefly', 5, 1.5, 'Transport', false),
('Nebuchadnezzar', 'The Matrix', 7, 2.0, 'Hovercraft', false),
('Rocinante', 'The Expanse', 50, 20.0, 'Gunship', true),
('Eagle 5', 'Spaceballs', 2, 0.5, 'Space RV', false),
('Heart of Gold', 'Hitchhikers Guide', 3, 42.0, 'Research', false),
('Discovery One', '2001: A Space Odyssey', 6, 1.0, 'Exploration', false),
('Event Horizon', 'Event Horizon', 20, 50.0, 'Research', true),
('Slave I', 'Star Wars', 1, 1000, 'Fighter', true),
('Red Dwarf', 'Red Dwarf', 169, 1.0, 'Mining Ship', false),
('Normandy SR-2', 'Mass Effect', 60, 15.0, 'Stealth', true),
('Elysium', 'Elysium', 100, 12.0, 'Space Habitat', false),
('Vipers', 'Battlestar Galactica', 1, 15.0, 'Fighter', true),
('Sulaco', 'Aliens', 85, 8.0, 'Warship', true),
('Planet Express Ship', 'Futurama', 4, 25.0, 'Delivery', true),
('Swordfish II', 'Cowboy Bebop', 1, 13.0, 'Fighter', true);

-- Fase 1: poblar la flota (20 naves) en 4 grupos de 5 (25% cada uno) para que la demo
-- se vea repartida de forma pareja entre los estados posibles:
--   * museo + teatro : 1 (Enterprise), 5 (Galactica), 11 (Discovery One), 14 (Red Dwarf), 16 (Elysium)
--   * solo museo     : 3 (X-Wing), 9 (Eagle 5), 12 (Event Horizon), 18 (Sulaco), 19 (Planet Express Ship)
--   * solo teatro    : 2 (Millennium Falcon), 6 (Serenity), 10 (Heart of Gold), 15 (Normandy SR-2), 20 (Swordfish II)
--   * sin asignar    : 4 (Y-Wing), 7 (Nebuchadnezzar), 8 (Rocinante), 13 (Slave I), 17 (Vipers)  -> quedan is_museum/is_theater = null

-- Museo + Teatro (25%)
UPDATE spacecrafts SET is_museum = true, is_theater = true, museum_capacity = 150 WHERE id = 1;  -- USS Enterprise
UPDATE spacecrafts SET is_museum = true, is_theater = true, museum_capacity = 300 WHERE id = 5;  -- Galactica
UPDATE spacecrafts SET is_museum = true, is_theater = true, museum_capacity = 80  WHERE id = 11; -- Discovery One
UPDATE spacecrafts SET is_museum = true, is_theater = true, museum_capacity = 120 WHERE id = 14; -- Red Dwarf
UPDATE spacecrafts SET is_museum = true, is_theater = true, museum_capacity = 500 WHERE id = 16; -- Elysium

-- Solo museo (25%)
UPDATE spacecrafts SET is_museum = true, museum_capacity = 20  WHERE id = 3;  -- X-Wing
UPDATE spacecrafts SET is_museum = true, museum_capacity = 30  WHERE id = 9;  -- Eagle 5
UPDATE spacecrafts SET is_museum = true, museum_capacity = 60  WHERE id = 12; -- Event Horizon
UPDATE spacecrafts SET is_museum = true, museum_capacity = 200 WHERE id = 18; -- Sulaco
UPDATE spacecrafts SET is_museum = true, museum_capacity = 40  WHERE id = 19; -- Planet Express Ship

-- Solo teatro (25%)
UPDATE spacecrafts SET is_theater = true WHERE id = 2;  -- Millennium Falcon
UPDATE spacecrafts SET is_theater = true WHERE id = 6;  -- Serenity
UPDATE spacecrafts SET is_theater = true WHERE id = 10; -- Heart of Gold
UPDATE spacecrafts SET is_theater = true WHERE id = 15; -- Normandy SR-2
UPDATE spacecrafts SET is_theater = true WHERE id = 20; -- Swordfish II

-- Sin asignar (25%): ids 4, 7, 8, 13, 17 se quedan como vinieron del INSERT (is_museum/is_theater = null)

-- Horarios de museo: una nave puede ser museo por el grupo "museo+teatro" o "solo museo".
-- Se generan con fechas relativas (CURRENT_DATE) para que siempre caigan dentro de la
-- ventana movil de 8 dias (hoy + 7) validada en MuseumScheduleService, sin importar
-- cuando Render reinicie la instancia.
INSERT INTO museum_schedules (spacecraft_id, schedule_date, open_time, close_time) VALUES
(1, CURRENT_DATE, '09:00:00', '18:00:00'),
(1, DATEADD('DAY', 1, CURRENT_DATE), '09:00:00', '18:00:00'),
(5, CURRENT_DATE, '10:00:00', '19:00:00'),
(5, DATEADD('DAY', 1, CURRENT_DATE), '10:00:00', '19:00:00'),
(11, CURRENT_DATE, '08:30:00', '17:00:00'),
(11, DATEADD('DAY', 1, CURRENT_DATE), '08:30:00', '17:00:00'),
(14, CURRENT_DATE, '11:00:00', '20:00:00'),
(14, DATEADD('DAY', 1, CURRENT_DATE), '11:00:00', '20:00:00'),
(16, CURRENT_DATE, '09:00:00', '21:00:00'),
(16, DATEADD('DAY', 1, CURRENT_DATE), '09:00:00', '21:00:00'),
(3, CURRENT_DATE, '09:00:00', '17:00:00'),
(3, DATEADD('DAY', 1, CURRENT_DATE), '09:00:00', '17:00:00'),
(9, CURRENT_DATE, '10:00:00', '16:00:00'),
(9, DATEADD('DAY', 1, CURRENT_DATE), '10:00:00', '16:00:00'),
(12, CURRENT_DATE, '12:00:00', '20:00:00'),
(12, DATEADD('DAY', 1, CURRENT_DATE), '12:00:00', '20:00:00'),
(18, CURRENT_DATE, '09:00:00', '18:00:00'),
(18, DATEADD('DAY', 1, CURRENT_DATE), '09:00:00', '18:00:00'),
(19, CURRENT_DATE, '11:00:00', '19:00:00'),
(19, DATEADD('DAY', 1, CURRENT_DATE), '11:00:00', '19:00:00');

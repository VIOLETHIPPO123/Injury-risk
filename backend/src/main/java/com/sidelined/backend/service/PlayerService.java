package com.sidelined.backend.service;

import com.sidelined.backend.model.Player;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.List;

@Service
public class PlayerService {

    // Four active players per team for the 2026-27 season. Names and teams are real;
    // snap counts are illustrative placeholders until real game data is wired in.
    public List<Player> getAllPlayers() {
        return Arrays.asList(
            new Player(1L, "Marvin Harrison Jr.", "ARI", 58, 236),
            new Player(2L, "Trey McBride", "ARI", 55, 228),
            new Player(3L, "Paris Johnson Jr.", "ARI", 68, 270),
            new Player(4L, "Walter Nolen", "ARI", 44, 120),

            new Player(5L, "Bijan Robinson", "ATL", 52, 196),
            new Player(6L, "Drake London", "ATL", 61, 244),
            new Player(7L, "Michael Penix Jr.", "ATL", 66, 262),
            new Player(8L, "Kyle Pitts", "ATL", 48, 204),

            new Player(9L, "Lamar Jackson", "BAL", 64, 258),
            new Player(10L, "Derrick Henry", "BAL", 30, 176),
            new Player(11L, "Zay Flowers", "BAL", 57, 222),
            new Player(12L, "Kyle Hamilton", "BAL", 70, 276),

            new Player(13L, "Josh Allen", "BUF", 67, 264),
            new Player(14L, "James Cook", "BUF", 46, 184),
            new Player(15L, "DJ Moore", "BUF", 59, 206),
            new Player(16L, "Dalton Kincaid", "BUF", 52, 140),

            new Player(17L, "Bryce Young", "CAR", 65, 258),
            new Player(18L, "Tetairoa McMillan", "CAR", 60, 236),
            new Player(19L, "Chuba Hubbard", "CAR", 41, 170),
            new Player(20L, "Derrick Brown", "CAR", 50, 196),

            new Player(21L, "Caleb Williams", "CHI", 68, 268),
            new Player(22L, "Rome Odunze", "CHI", 62, 150),
            new Player(23L, "Colston Loveland", "CHI", 49, 190),
            new Player(24L, "D'Andre Swift", "CHI", 38, 156),

            new Player(25L, "Joe Burrow", "CIN", 66, 264),
            new Player(26L, "Ja'Marr Chase", "CIN", 62, 240),
            new Player(27L, "Tee Higgins", "CIN", 57, 220),
            new Player(28L, "Chase Brown", "CIN", 48, 180),

            new Player(29L, "Quinshon Judkins", "CLE", 45, 172),
            new Player(30L, "Jerry Jeudy", "CLE", 56, 228),
            new Player(31L, "Denzel Ward", "CLE", 64, 250),
            new Player(32L, "Jared Verse", "CLE", 50, 198),

            new Player(33L, "Dak Prescott", "DAL", 69, 272),
            new Player(34L, "CeeDee Lamb", "DAL", 60, 232),
            new Player(35L, "George Pickens", "DAL", 58, 210),
            new Player(36L, "Quinnen Williams", "DAL", 44, 186),

            new Player(37L, "Bo Nix", "DEN", 67, 266),
            new Player(38L, "Jaylen Waddle", "DEN", 54, 132),
            new Player(39L, "Courtland Sutton", "DEN", 58, 236),
            new Player(40L, "Pat Surtain II", "DEN", 66, 262),

            new Player(41L, "Jared Goff", "DET", 64, 256),
            new Player(42L, "Amon-Ra St. Brown", "DET", 61, 240),
            new Player(43L, "Jahmyr Gibbs", "DET", 42, 178),
            new Player(44L, "Aidan Hutchinson", "DET", 55, 150),

            new Player(45L, "Jordan Love", "GB", 63, 254),
            new Player(46L, "Josh Jacobs", "GB", 44, 184),
            new Player(47L, "Tucker Kraft", "GB", 53, 206),
            new Player(48L, "Matthew Golden", "GB", 50, 192),

            new Player(49L, "C.J. Stroud", "HOU", 65, 262),
            new Player(50L, "Nico Collins", "HOU", 57, 228),
            new Player(51L, "Will Anderson Jr.", "HOU", 52, 208),
            new Player(52L, "Derek Stingley Jr.", "HOU", 66, 260),

            new Player(53L, "Daniel Jones", "IND", 66, 264),
            new Player(54L, "Jonathan Taylor", "IND", 50, 196),
            new Player(55L, "Tyler Warren", "IND", 54, 214),
            new Player(56L, "Sauce Gardner", "IND", 63, 250),

            new Player(57L, "Trevor Lawrence", "JAX", 66, 262),
            new Player(58L, "Travis Hunter", "JAX", 52, 150),
            new Player(59L, "Brian Thomas Jr.", "JAX", 60, 236),
            new Player(60L, "Josh Hines-Allen", "JAX", 53, 210),

            new Player(61L, "Patrick Mahomes", "KC", 64, 256),
            new Player(62L, "Kenneth Walker III", "KC", 45, 176),
            new Player(63L, "Travis Kelce", "KC", 50, 208),
            new Player(64L, "Chris Jones", "KC", 47, 190),

            new Player(65L, "Justin Herbert", "LAC", 67, 266),
            new Player(66L, "Ladd McConkey", "LAC", 58, 228),
            new Player(67L, "Omarion Hampton", "LAC", 26, 164),
            new Player(68L, "Derwin James Jr.", "LAC", 68, 268),

            new Player(69L, "Matthew Stafford", "LAR", 63, 254),
            new Player(70L, "Puka Nacua", "LAR", 61, 242),
            new Player(71L, "Kyren Williams", "LAR", 49, 192),
            new Player(72L, "Myles Garrett", "LAR", 52, 204),

            new Player(73L, "Fernando Mendoza", "LV", 40, 112),
            new Player(74L, "Ashton Jeanty", "LV", 51, 196),
            new Player(75L, "Brock Bowers", "LV", 57, 224),
            new Player(76L, "Maxx Crosby", "LV", 58, 228),

            new Player(77L, "Malik Willis", "MIA", 65, 248),
            new Player(78L, "De'Von Achane", "MIA", 47, 182),
            new Player(79L, "Jordyn Brooks", "MIA", 68, 270),
            new Player(80L, "Zach Sieler", "MIA", 49, 196),

            new Player(81L, "J.J. McCarthy", "MIN", 62, 250),
            new Player(82L, "Kyler Murray", "MIN", 12, 96),
            new Player(83L, "Justin Jefferson", "MIN", 63, 246),
            new Player(84L, "Jordan Addison", "MIN", 58, 226),

            new Player(85L, "Drake Maye", "NE", 68, 266),
            new Player(86L, "TreVeyon Henderson", "NE", 44, 170),
            new Player(87L, "Will Campbell", "NE", 69, 272),
            new Player(88L, "Christian Gonzalez", "NE", 65, 256),

            new Player(89L, "Tyler Shough", "NO", 66, 260),
            new Player(90L, "Alvin Kamara", "NO", 34, 172),
            new Player(91L, "Chris Olave", "NO", 59, 232),
            new Player(92L, "Chase Young", "NO", 47, 188),

            new Player(93L, "Jaxson Dart", "NYG", 67, 264),
            new Player(94L, "Cam Skattebo", "NYG", 60, 148),
            new Player(95L, "Malik Nabers", "NYG", 62, 244),
            new Player(96L, "Abdul Carter", "NYG", 54, 212),

            new Player(97L, "Breece Hall", "NYJ", 46, 180),
            new Player(98L, "Garrett Wilson", "NYJ", 61, 240),
            new Player(99L, "Mason Taylor", "NYJ", 50, 196),
            new Player(100L, "Minkah Fitzpatrick", "NYJ", 69, 274),

            new Player(101L, "Jalen Hurts", "PHI", 65, 258),
            new Player(102L, "Saquon Barkley", "PHI", 47, 168),
            new Player(103L, "DeVonta Smith", "PHI", 60, 238),
            new Player(104L, "Jalen Carter", "PHI", 51, 202),

            new Player(105L, "Aaron Rodgers", "PIT", 64, 254),
            new Player(106L, "DK Metcalf", "PIT", 59, 232),
            new Player(107L, "Michael Pittman Jr.", "PIT", 57, 222),
            new Player(108L, "T.J. Watt", "PIT", 56, 150),

            new Player(109L, "Sam Darnold", "SEA", 65, 260),
            new Player(110L, "Jaxon Smith-Njigba", "SEA", 62, 244),
            new Player(111L, "Devon Witherspoon", "SEA", 66, 262),
            new Player(112L, "Nick Emmanwori", "SEA", 64, 250),

            new Player(113L, "Brock Purdy", "SF", 66, 262),
            new Player(114L, "Christian McCaffrey", "SF", 58, 212),
            new Player(115L, "Mike Evans", "SF", 49, 120),
            new Player(116L, "Nick Bosa", "SF", 53, 210),

            new Player(117L, "Baker Mayfield", "TB", 67, 266),
            new Player(118L, "Bucky Irving", "TB", 45, 178),
            new Player(119L, "Emeka Egbuka", "TB", 60, 236),
            new Player(120L, "Tristan Wirfs", "TB", 70, 276),

            new Player(121L, "Cam Ward", "TEN", 68, 268),
            new Player(122L, "Tony Pollard", "TEN", 43, 170),
            new Player(123L, "Calvin Ridley", "TEN", 57, 226),
            new Player(124L, "Jeffery Simmons", "TEN", 50, 198),

            new Player(125L, "Jayden Daniels", "WAS", 66, 260),
            new Player(126L, "Terry McLaurin", "WAS", 59, 232),
            new Player(127L, "Jacory Croskey-Merritt", "WAS", 39, 150),
            new Player(128L, "Frankie Luvu", "WAS", 64, 252)
        );
    }
}

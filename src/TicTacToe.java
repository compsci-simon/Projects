import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.util.*;
import java.io.File;

public class TicTacToe {

    public static String[] fileString;
    public static Stack[][] board = new Stack[4][4];
    public static Stack[] green_stack = new Stack[3];
    public static Stack[] blue_stack = new Stack[3];
    public static Stack stack_bag = new Stack();
    public static int[] blue_specials = {2, 2, 2, 2};
    public static int[] green_specials = {2, 2, 2, 2};
    public static boolean input_file_ended = false;
    public static int BLUE = 'B';
    public static int GREEN = 'G';
    public static int[] powerPieces = {2, 2, 2, 2};
    public static double height = 800;
    public static double width = height/2;
    public static double board_height = height/2;
    public static double pen_thickness = 0.004;
    public static double[] circle_radii = {40, 30, 20, 10};
    public static double circle1_radius = 40;
    public static double circle2_radius = 30;
    public static double circle3_radius = 20;
    public static double circle4_radius = 10;

    public static void main(String[] args) throws FileNotFoundException {

        initialize();
        if (args.length == 1) {
            /** Text Mode */
            System.out.println(args[0]);

            File file = new File(args[0]);
            Scanner scanner = new Scanner(file);
            int lines = 0;

            while (scanner.hasNextLine()) {
                lines++;
                scanner.nextLine();
            }

            fileString = new String[lines];
            scanner = new Scanner(file);
            int line = 0;
            while (scanner.hasNextLine()) {
                fileString[line] = scanner.nextLine();
                line++;
            }

            printBoard();

            play_moves();

            gameStats();

            // example methods to use from the In.java class provided in stdlib

        } else {
            GUI();
        }
    }

    public static void GUI() {
        drawBoard();
        char player = 'G';

        while (true) {
            player = opponent(player);
            draw_circles(player);
            drawSpecials(player);
            player_move_gui(player);
            printBoard();
            drawGame();
            String win = winner();
            if (!win.equals("No winner")) {
                message(win + " Press any key to quit.");
                while (true) {
                    if (StdDraw.hasNextKeyTyped()) {
                        System.exit(0);
                    }
                }
            }
        }
    }

    public static void drawBoard() {
        StdDraw.setCanvasSize((int)width, (int)height);
        StdDraw.setXscale(0, width);
        StdDraw.setYscale(0, height);
        StdDraw.setPenRadius(pen_thickness);

        // Horizontal lines
        for (int i = 0; i < 5; i++) {
            StdDraw.line(0, i*board_height/4, width, i*board_height/4);
        }

        // Vertival lines
        for (int i = 0; i < 4; i++) {
            StdDraw.line(i*width/4, 0, i*width/4, board_height);
        }

        StdDraw.show();
    }

    public static void player_move_gui(char player) {


        Stack[] gen_stack;
        if (player == 'B') {
            gen_stack = blue_stack;
        } else {
            gen_stack = green_stack;
        }
        char key;
        int type = -1;
        int stack_col;
        message("Enter ABCwasdqtbrp");
        while (true) {
            while (!StdDraw.hasNextKeyTyped()) {

            }
            key = StdDraw.nextKeyTyped();
            Character.toString(key);
            if ("ABCwasdqtbrp".contains(Character.toString(key))) {
                if ("ABC".contains(Character.toString(key))) {
                    type = 1;
                } else if ("wasd".contains(Character.toString(key))) {
                    type = 2;
                } else if ("tbrp".contains(Character.toString(key))) {
                    type = 3;
                } else if (key == 'q') {
                    System.out.println("You quit the game");
                    System.exit(0);
                }
                break;
            }
        }
        String move;
        int[] coords1;
        int[] coords2;
        switch(type) {
            case 1:
                switch (key) {
                    case 'A':
                        stack_col = 0;
                        break;
                    case 'B':
                        stack_col = 1;
                        break;
                    default:
                        stack_col = 2;
                        break;
                }
                if (gen_stack[stack_col].size() == 0) {
                    message("Invalid move");
                }
                message("Move with wasd");
                int[] coords = selectCoords();

                drawBlock(coords[0], coords[1], 'b');
                stack_col++;
                move = "" + stack_col + " " + coords[0] + " " + coords[1];
                System.out.println(move);
                if (legal_move(move, player)) {
                    play_move(move, player);
                } else {
                    message("Invalid move");
                }
                break;
            case 2:
                coords1 = selectCoords();
                coords2 = selectCoords();
                move = "4 "+coords1[0]+" "+coords1[1]+" "+coords2[0]+" "+coords2[1];
                if (legal_move(move, player)) {
                    play_move(move, player);
                } else {
                    message("Invalid move");
                }
                break;
            case 3:
                switch (key) {
                    case 't':
                        coords1 = selectCoords();
                        message("Pieces 1||2||3||4");
                        while (true) {
                            while (!StdDraw.hasNextKeyTyped()) {
                            }
                            key = StdDraw.nextKeyTyped();
                            Character.toString(key);
                            if ("1234".contains(Character.toString(key))) {
                                break;
                            }
                        }
                        move = "5 T "+coords1[0]+" "+coords1[1]+" "+key;
                        if (legal_move(move, player)) {
                            play_move(move, player);
                        } else {
                            message("Invalid move");
                        }
                        break;
                    case 'b':
                        coords1 = selectCoords();
                        move = "5 B "+coords1[0]+" "+coords1[1];
                        if (legal_move(move, player)) {
                            play_move(move, player);
                        } else {
                            message("Invalid move");
                        }
                        break;
                    case 'r':
                        coords1 = selectCoords();
                        message("Direction 0||1");
                        while (true) {
                            while (!StdDraw.hasNextKeyTyped()) {
                            }
                            key = StdDraw.nextKeyTyped();
                            Character.toString(key);
                            if ("01".contains(Character.toString(key))) {
                                break;
                            }
                        }
                        move = "5 S "+coords1[0]+" "+coords1[1]+" "+key;
                        if (legal_move(move, player)) {
                            play_move(move, player);
                        } else {
                            message("Invalid move");
                        }
                        break;
                    case 'p':
                        message("Invalid move");
                        break;
                }
                break;
        }




    }

    public static void drawGame() {

        StdDraw.setPenColor(StdDraw.WHITE);
        StdDraw.filledSquare(width/2, board_height/2, width/2);
        StdDraw.setPenColor(StdDraw.BLACK);
        // Horizontal lines
        for (int i = 0; i < 5; i++) {
            StdDraw.line(0, i*board_height/4, width, i*board_height/4);
        }

        // Vertival lines
        for (int i = 0; i < 4; i++) {
            StdDraw.line(i*width/4, 0, i*width/4, board_height);
        }

        for (int i = 0; i < 4; i ++) {
            for (int j = 0; j < 4; j++) {
                if (!board[i][j].peek().toString().equals("__")) {
                    String piece = board[i][j].peek().toString();
                    if (piece.equals("SH")) {
                        drawFilledSquar(i+1, j+1, 'r');
                    } else {
                        char player;
                        int size;
                        if (piece.startsWith("B")) {
                            player = 'B';
                        } else {
                            player = 'G';
                        }
                        size = pieceSize(piece);
                        drawCircle(i+1, j+1, player, size);
                    }
                }
            }
        }
    }

    public static void drawFilledSquar(int row, int col, char colour) {
        switch (colour) {
            case 'r':
                StdDraw.setPenColor(StdDraw.BOOK_RED);
                break;
        }
        StdDraw.filledSquare(width/8 + (col-1) * width/4, board_height - board_height/8 - (row-1) * board_height/4, 49);
        StdDraw.show();
    }

    public static void drawSpecials(char player) {
        StdDraw.setPenColor(StdDraw.BLACK);
        int square_r = 40;
        int x_offset = 25;
        int y_offset = 25;

        StdDraw.setPenColor(StdDraw.WHITE);
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 4; j++) {

                StdDraw.filledSquare(x_offset + square_r + j * (2 * square_r),
                        board_height + y_offset + square_r + i * (2 * square_r), square_r+1);
            }
        }

        StdDraw.setPenColor(StdDraw.BLACK);
        int[] gen_specials;
        if (player == 'B') {
            gen_specials = blue_specials;
        } else {
            gen_specials = green_specials;
        }
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 4; j++) {
                StdDraw.square(x_offset + square_r + j*(2*square_r),
                        board_height + y_offset + square_r + i*(2*square_r), square_r);
                String file = "";
                switch(j) {
                    case 0:
                        file = "transporter.png";
                        break;
                    case 1:
                        file = "bomb.png";
                        break;
                    case 2:
                        file = "shifter.png";
                        break;
                    case 3:
                        file = "paintbrush.png";
                        break;
                }
                if (gen_specials[j] - i > 0) {
                    StdDraw.picture(x_offset + square_r + j*(2*square_r),
                            board_height + y_offset + square_r + i*(2*square_r), "./lib/"+file, 2*square_r, 2*square_r);
                }

            }
        }

    }

    public static int[] selectCoords() {
        int row = 1;
        int col = 1;
        drawBlock(1, 1, 'r');
        while (true) {
            if (StdDraw.hasNextKeyTyped()) {
                char key = StdDraw.nextKeyTyped();
                if (key == '\n') {
                    int[] coords = {row, col};
                    return coords;
                }
                drawBlock(row, col, 'b');
                switch (key) {
                    case 'w':
                        row--;
                        break;
                    case 'a':
                        col--;
                        break;
                    case 's':
                        row++;
                        break;
                    case 'd':
                        col++;
                        break;
                }
                if (col < 1) {
                    col = 4;
                } else if (col > 4) {
                    col = 1;
                }
                if (row < 1) {
                    row = 1;
                } else if (row > 4) {
                    row = 4;
                }
                drawBlock(row, col, 'r');

            }
        }
    }

    public static void drawCircle(int row, int col, char player, int size) {
        switch (player) {
            case 'B':
                StdDraw.setPenColor(StdDraw.BOOK_LIGHT_BLUE);
                break;
            case 'G':
                StdDraw.setPenColor(StdDraw.GREEN);
                break;
        }
        StdDraw.filledCircle(width/8 + (col-1) * width/4, board_height - board_height/8 - (row-1) * board_height/4, circle_radii[size-1]);
        StdDraw.show();
    }

    public static void drawBlock(int row, int col, char color) {
        switch (color) {
            case 'r':
                StdDraw.setPenColor(StdDraw.RED);
                break;
            case 'b':
                StdDraw.setPenColor(StdDraw.BLACK);
                break;
        }
        StdDraw.square(width/8 + (col-1) * width/4, board_height - board_height/8 - (row-1) * board_height/4, width/8);
        StdDraw.show();
    }

    public static void draw_circles(char player) {

        StdDraw.setPenColor(StdDraw.WHITE);
        StdDraw.filledCircle(10 + circle_radii[0], height - circle_radii[0] - 10, circle_radii[0]);
        StdDraw.filledCircle(20 + circle_radii[0] + 2*circle_radii[0], height - circle_radii[0] - 10, circle_radii[0]);
        StdDraw.filledCircle(30 + circle_radii[0] + 4*circle_radii[0], height - circle_radii[0] - 10, circle_radii[0]);

        if (player == 'B') {
            int size;
            StdDraw.setPenColor(StdDraw.BOOK_LIGHT_BLUE);
            size = pieceSize(blue_stack[0].peek().toString()) - 1;
            StdDraw.filledCircle(10 + circle_radii[0], height - circle_radii[0] - 10, circle_radii[size]);
            size = pieceSize(blue_stack[1].peek().toString()) - 1;
            StdDraw.filledCircle(20 + circle_radii[0] + 2*circle_radii[0], height - circle_radii[0] - 10, circle_radii[size]);
            size = pieceSize(blue_stack[2].peek().toString()) - 1;
            StdDraw.filledCircle(30 + circle_radii[0] + 4*circle_radii[0], height - circle_radii[0] - 10, circle_radii[size]);
        } else {
            int size;
            StdDraw.setPenColor(StdDraw.GREEN);
            size = pieceSize(green_stack[0].peek().toString()) - 1;
            StdDraw.filledCircle(10 + circle_radii[0], height - circle_radii[0] - 10, circle_radii[size]);
            size = pieceSize(green_stack[1].peek().toString()) - 1;
            StdDraw.filledCircle(20 + circle_radii[0] + 2*circle_radii[0], height - circle_radii[0] - 10, circle_radii[size]);
            size = pieceSize(green_stack[2].peek().toString()) - 1;
            StdDraw.filledCircle(30 + circle_radii[0] + 4*circle_radii[0], height - circle_radii[0] - 10, circle_radii[size]);
        }
        StdDraw.show();
    }

    public static String winner() {
        boolean blueWin = false;
        boolean greenWin = false;
        int bcount = 0;
        int gcount = 0;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                String piece = board[i][j].peek().toString();
                if (piece.charAt(0) == 'B') {
                    bcount++;
                }
                if (piece.charAt(0) == 'G') {
                    gcount++;
                }
            }
            if (bcount == 4) {
                blueWin = true;
            }
            if (gcount == 4) {
                greenWin = true;
            }
            bcount = 0;
            gcount = 0;
            for (int j = 0; j < 4; j++) {
                String piece = board[j][i].peek().toString();
                if (piece.charAt(0) == 'B') {
                    bcount++;
                }
                if (piece.charAt(0) == 'G') {
                    gcount++;
                }
            }
            if (bcount == 4) {
                blueWin = true;
            }
            if (gcount == 4) {
                greenWin = true;
            }
            bcount = 0;
            gcount = 0;
        }
        int row = 0;
        int col = 0;
        for (int i = 0; i < 4; i++) {
            String piece = board[row][col].peek().toString();
            if (piece.charAt(0) == 'B') {
                bcount++;
            }
            if (piece.charAt(0) == 'G') {
                gcount++;
            }
            row++;
            col++;
        }
        if (bcount == 4) {
            blueWin = true;
        }
        if (gcount == 4) {
            greenWin = true;
        }
        bcount = 0;
        gcount = 0;
        row = 3;
        col = 0;
        for (int i = 0; i < 4; i++) {
            String piece = board[row][col].peek().toString();
            if (piece.charAt(0) == 'B') {
                bcount++;
            }
            if (piece.charAt(0) == 'G') {
                gcount++;
            }
            row--;
            col++;
        }
        if (bcount == 4) {
            blueWin = true;
        }
        if (gcount == 4) {
            greenWin = true;
        }
        if (blueWin && greenWin) {
            return "Draw!";
        } else if (blueWin) {
            return "Blue wins!";
        } else if (greenWin) {
            return "Green wins!";
        }else {
            return "No winner";
        }
    }

    public static void gameStats() {
        String winner = winner();
        if (input_file_ended && winner.equals("No winner")) {
            winner = winner + ", input file ended";
        }
        System.out.println(winner);
        System.out.println("Statistics for each stack present on the board");
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                if (board[i][j].size() > 1) {
                    while (board[i][j].size() > 1) {
                        System.out.printf("%s ", board[i][j].pop().toString());
                    }
                    System.out.printf("\n");
                }
            }
        }
        System.out.println("Statistics for extrernal stacks blue player");
        for (int i = 0; i < 3; i++) {
            System.out.printf("Stack %d:", i + 1);
            while (blue_stack[i].size() > 0) {
                System.out.printf(" %s", blue_stack[i].pop());
            }
            System.out.printf("\n");
        }
        System.out.println("Statistics for extrernal stacks green player");
        for (int i = 0; i < 3; i++) {
            System.out.printf("Stack %d:", i + 1);
            while (green_stack[i].size() > 0) {
                System.out.printf(" %s", green_stack[i].pop());
            }
            System.out.printf("\n");
        }

    }

    public static void initialize() {

        for(int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                board[i][j] = new Stack();
                board[i][j].push("__");
            }
        }

        for (int i = 0; i < 3; i++) {
            blue_stack[i] = new Stack();
            green_stack[i] = new Stack();
            blue_stack[i].push("B4");
            blue_stack[i].push("B3");
            blue_stack[i].push("B2");
            blue_stack[i].push("B1");
            green_stack[i].push("G4");
            green_stack[i].push("G3");
            green_stack[i].push("G2");
            green_stack[i].push("G1");
        }
    }

    public static void play_moves() {
        char player = '_';
        for (int i = 0; i < fileString.length; i++) {
            player = playerTurn(i);
            if (legal_move(fileString[i], player)) {
                play_move(fileString[i], player);
                printBoard();
            } else {
                System.out.println("Invalid move");
            }
            String win = winner();
            if (!win.equals("No winner")) {
                return;
            }
        }
        input_file_ended = true;
    }

    public static void play_move(String move, char player) {
        String[] moveComponents = move.split(" ");
        if (moveComponents.length == 3) {

            int row = Integer.parseInt(moveComponents[1]) - 1;
            int col = Integer.parseInt(moveComponents[2]) - 1;
            if (board[row][col].peek().toString().equals("SH")) {
                board[row][col].pop();
                board[row][col].push("__");
            } else{
                int stack_col = Integer.parseInt(moveComponents[0]) - 1;
                String piece;
                if (player == 'B') {
                    // Blue player
                    piece = blue_stack[stack_col].pop().toString();
                } else {
                    // Green player
                    piece = green_stack[stack_col].pop().toString();
                }
                board[row][col].push(piece);
            }
        } else if (Integer.parseInt(moveComponents[0]) == 4) {
            int row_from = Integer.parseInt(moveComponents[1]) - 1;
            int col_from = Integer.parseInt(moveComponents[2]) - 1;
            int row_to = Integer.parseInt(moveComponents[3]) - 1;
            int col_to = Integer.parseInt(moveComponents[4]) - 1;
            String piece = board[row_from][col_from].pop().toString();
            if (board[row_to][col_to].peek().toString().equals("SH")) {
                pieceToStack(piece);
                board[row_to][col_to].pop();
                board[row_to][col_to].push("__");
            } else {
                board[row_to][col_to].push(piece);
            }
        } else if (Integer.parseInt(moveComponents[0]) == 5) {
            // Doing a special move
            char type = moveComponents[1].charAt(0);
            int row = Integer.parseInt(moveComponents[2]) - 1;
            int col = Integer.parseInt(moveComponents[3]) - 1;

            switch (type) {
                case 'T':
                    int depth = Integer.parseInt(moveComponents[4]);
                    play_transporter(row, col, depth, player);

                    break;
                case 'S':
                    int dir = Integer.parseInt(moveComponents[4]);
                    play_shifter(row, col, dir, player);
                    break;
                case 'B':
                    // Play the bomb
                    play_bomb(row, col, player);
                    break;
            }
        }
    }

    public static void message(String message) {
        StdDraw.setPenColor(StdDraw.BLACK);
        StdDraw.filledRectangle(width/2, board_height + board_height/2 + 50, 150, 50);
        StdDraw.setPenColor(StdDraw.WHITE);
        StdDraw.text(width/2, board_height + board_height/2 + 50, message);
    }

    public static void play_shifter(int row, int col, int dir, int player) {
        if (player == 'B') {
            blue_specials[2]--;
        } else {
            green_specials[2]--;
        }

        int tiles[][] = new int[8][2];
        int moves[][] = {
                {-1, -1},
                {-1, 0},
                {-1, 1},
                {0, 1},
                {1, 1},
                {1, 0},
                {1, -1},
                {0, -1}
        };

        for (int i = 0; i < 8; i++) {
            int current_row = row + moves[i][0];
            int current_col = col + moves[i][1];
            if (current_row == -1) {
                current_row = 3;
            }
            if (current_col == -1) {
                current_col = 3;
            }
            if (current_row == 4) {
                current_row = 0;
            }
            if (current_col == 4) {
                current_col = 0;
            }
            tiles[i][0] = current_row;
            tiles[i][1] = current_col;
        }
//                    for (int i = 0; i < 8; i++) {
//                        System.out.printf("%d %d\n", tiles[i][0], tiles[i][1]);
//                    }
        if (dir == 0) {
            Stack temp = board[tiles[7][0]][tiles[7][1]];
            for (int i = 6; i >= 0; i--) {
                int curr_row = tiles[i][0];
                int curr_col = tiles[i][1];
                int next_row = tiles[i + 1][0];
                int next_col = tiles[i + 1][1];
                board[next_row][next_col] = board[curr_row][curr_col];
            }
            board[tiles[0][0]][tiles[0][1]] = temp;
        } else {
            Stack temp = board[tiles[0][0]][tiles[0][1]];
            for (int i = 1; i <= 7; i++) {
                int curr_row = tiles[i][0];
                int curr_col = tiles[i][1];
                int prev_row = tiles[i - 1][0];
                int prev_col = tiles[i - 1][1];
                board[prev_row][prev_col] = board[curr_row][curr_col];
            }
            board[tiles[7][0]][tiles[7][1]] = temp;
        }
    }

    public static void play_transporter(int row, int col, int depth, char player) {
        if (player == 'B') {
            blue_specials[0]--;
        } else {
            green_specials[0]--;
        }
        int selected_row = -1;
        int selected_col = -1;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                if (board[i][j].size() == 1) {
                    if (selected_row == -1) {
                        selected_row = i;
                        selected_col = j;
                    }
                    int new_dist = Math.abs(row - i) + Math.abs(col - j);
                    int old_dist = Math.abs(row - selected_row) + Math.abs(col - selected_col);
                    if (new_dist < old_dist) {
                        selected_row = i;
                        selected_col = j;
                    }
                }
            }
        }

        if (selected_row == -1) {
            // Do nothing. No empty tiles on board
            System.out.println("Board full");
        } else {
            Stack temp = new Stack();
            for (int i =0; i < depth; i++) {
                String piece = board[row][col].pop().toString();
                temp.push(piece);
            }

            for (int i =0; i < depth; i++) {
                String piece = temp.pop().toString();
                board[selected_row][selected_col].push(piece);
            }
        }
    }

    public static void play_bomb(int row, int col, char player) {
        if (player == 'B') {
            blue_specials[1]--;
        } else {
            green_specials[1]--;
        }
        int movesb[][] = {
                {-1, -1},
                {-1, 0},
                {-1, 1},
                {0, 1},
                {1, 1},
                {1, 0},
                {1, -1},
                {0, -1}
        };
        while (board[row][col].size() > 1) {
            String piece = board[row][col].pop().toString();
            stack_bag.push(piece);
        }
        for (int i = 0; i < 8; i++) {
            int temp_row = row + movesb[i][0];
            int temp_col = col + movesb[i][1];
            if (temp_row == -1) {
                temp_row = 3;
            }
            if (temp_col == -1) {
                temp_col = 3;
            }
            if (temp_row == 4) {
                temp_row = 0;
            }
            if (temp_col == 4) {
                temp_col = 0;
            }
            if (board[temp_row][temp_col].size() > 1) {
                String piece = board[temp_row][temp_col].pop().toString();
                stack_bag.push(piece);
            } else {
                board[temp_row][temp_col].pop();
                board[temp_row][temp_col].push("SH");
            }
        }
        move_bag_to_stacks();
    }

    public static void move_bag_to_stacks() {
        if (stack_bag.size() <= 0) {
            return;
        }
        System.out.println(stack_bag.size());
        while (stack_bag.size() > 0) {

            String piece = stack_bag.pop().toString();
            pieceToStack(piece);

        }

    }

    public static void pieceToStack(String piece) {

        Stack temp_stack = new Stack();
        Stack gen_stack[];
        if (piece.charAt(0) == 'B') {
            gen_stack = blue_stack;
        } else {
            gen_stack = green_stack;
        }
        for (int i = 0; i < 3; i++) {
            if (gen_stack[i].size() == 4) {
                continue;
            } else {
                String[] t_array = new String[4];
                for (int j = 0; j < 4; j++) {
                    t_array[j] = "";
                }

                while(gen_stack[i].size() > 0) {
                    String tp = gen_stack[i].pop().toString();
                    int tp_size = pieceSize(tp);
                    t_array[tp_size - 1] = tp;
                }
                int piece_size = pieceSize(piece);
                if (t_array[piece_size - 1].equals("")) {
                    t_array[piece_size - 1] = piece;
                }
                for (int j = 3; j >= 0; j--) {
                    if (!t_array[j].equals("")) {
                        gen_stack[i].push(t_array[j]);
                    }
                }
            }
        }
    }

    public static char playerTurn(int i) {
        if (i%2 == 0) {
            return 'B';
        } else {
            return 'G';
        }
    }

    public static int pieceSize(String piece) {
        return piece.charAt(1) - 48;
    }

    public static boolean legal_move(String move, char player) {
        String[] line_components = move.split(" ");
        if (Integer.parseInt(line_components[0]) == -1) {
            System.out.println("Quit");
            gameStats();
            System.exit(0);
        }
        if (line_components.length == 3) {
            // Moving STACK piece to board
            int stack_col = Integer.parseInt(line_components[0]) - 1;
            int row = Integer.parseInt(line_components[1]) - 1;
            int col = Integer.parseInt(line_components[2]) - 1;
            if (board[row][col].peek().toString().equals("__") ||
                    board[row][col].peek().toString().equals("SH")) {
                return true;
            }
            if (stack_col < 0 || stack_col > 2 || row < 0 || row > 3 || col < 0 || col > 3) {
                return false;
            }
            String board_piece = board[row][col].peek().toString();
            int stack_piece_size;
            if (player == BLUE) {
                // Player is blue
                if (blue_stack[stack_col].size() == 0) {
                    return false;
                }
                stack_piece_size = pieceSize(blue_stack[stack_col].peek().toString());
                if (board_piece.equals("__")) {
                    return true;
                } else {
                    if (board_piece.charAt(0) == player) {
                        // Placing my piece on another of my pieces
                        return false;
                    } else if (board_piece.charAt(0) == opponent(player)) {
                        // Placing piece on top of opponent piece
                        int board_piece_size = pieceSize(board_piece);
                        if (stack_piece_size < board_piece_size) {
                            if (threeColumn(col, opponent(player)) ||
                                    threeRow(row, opponent(player)) ||
                                    threeDiagonal(row, col, opponent(player))) {
                                return true;
                            } else {
                                return false;
                            }
                        } else {
                            return false;
                        }
                    } else {
                        if (!board[row][col].peek().toString().equals("SH")) {
                            System.out.println("We should not be here ever");
                            System.out.println(board[row][col].peek().toString());
                        }
                        return false;
                    }
                }
            } else {
                // Player is green
                if (green_stack[stack_col].size() == 0) {
                    return false;
                }
                stack_piece_size = pieceSize(green_stack[stack_col].peek().toString());
                if (board_piece.equals("__")) {
                    return true;
                } else {
                    if (board_piece.charAt(0) == player) {
                        // Placing my piece on another of my pieces
                        return false;
                    } else if (board_piece.charAt(0) == opponent(player)) {
                        // Placing piece on top of opponent piece
                        int board_piece_size = pieceSize(board_piece);
                        if (stack_piece_size < board_piece_size) {
                            if (threeColumn(col, opponent(player)) ||
                                    threeRow(row, opponent(player)) ||
                                    threeDiagonal(row, col, opponent(player))) {
                                return true;
                            } else {
                                return false;
                            }
                        } else {
                            return false;
                        }
                    } else {
                        if (!board[row][col].peek().toString().equals("SH")) {
                            System.out.println("We should not be here ever");
                            System.out.println(board[row][col].peek().toString());
                        }
                        return false;
                    }
                }
            }
        } else if (Integer.parseInt(line_components[0]) == 4) {
            // Moving piece that is ALREADY on board
            int row_from = Integer.parseInt(line_components[1]) - 1;
            int col_from = Integer.parseInt(line_components[2]) - 1;
            int row_to = Integer.parseInt(line_components[3]) - 1;
            int col_to = Integer.parseInt(line_components[4]) - 1;
            if (row_from == row_to && col_from == col_to) {
                // Moving piece to its current position
                return false;
            }
            int min = Math.min(Math.min(row_from, col_from),Math.min(row_to, col_to));
            int max = Math.max(Math.max(row_from, col_from),Math.max(row_to, col_to));
            if (min < 0 || max > 3) {
                return false;
            }
            String piece_from = board[row_from][col_from].peek().toString();
            String piece_to = board[row_to][col_to].peek().toString();
            if (piece_from.charAt(0) != player) {
                // Trying to move one of the opponenets pieces
                return false;
            }
            int piece_from_size = pieceSize(piece_from);
            if (piece_to.equals("__") || piece_to.equals("SH")) {
                return true;
            }
            int piece_to_size = pieceSize(piece_to);
            if (piece_to.charAt(0) == player) {
                // Moving piece on top of my own piece
                if (piece_to_size > piece_from_size) {
                    return true;
                } else {
                    return false;
                }
            } else if (piece_to.charAt(0) == opponent(player)) {
                // Moving piece on top of opponent piece
                if (piece_to_size > piece_from_size) {
                    return true;
                } else {
                    return false;
                }
            }

        } else if (Integer.parseInt(line_components[0]) == 5) {
            // Doing a special move
            char type = line_components[1].charAt(0);
            int row = Integer.parseInt(line_components[2]) - 1;
            int col = Integer.parseInt(line_components[3]) - 1;
            String piece = board[row][col].peek().toString();
            if (row < 0 || col < 0) {
                return false;
            }

            switch (type) {
                case 'T':
                    // Transporter
                    int depth = Integer.parseInt(line_components[4]);
                    if (powerPieces[0] <= 0) {
                        return false;
                    }
                    if (board[row][col].size() == 1) {
                        // Cant transport an empty tile
                        return false;
                    }
                    if (piece.charAt(0) != opponent(player)) {
                        // Trying to move anything else than an opponents piece
                        return false;
                    } else {
                        if (depth >= board[row][col].size()) {
                            // Selected too many pieces
                            return false;
                        } else {
                            return true;
                        }
                    }
                case 'S':
                    // Shifter
                    if (powerPieces[3] <= 0) {
                        return false;
                    }
                    if (board[row][col].size() != 1) {
                        // Shifter can only be placed on empty tiles
                        return false;
                    } else {
                        return true;
                    }
                case 'B':
                    // Bomb
                    if (powerPieces[1] <= 0) {
                        return false;
                    }
                    if (board[row][col].peek().toString().charAt(0) != player) {
                        // Can only put a bomb on my own piece
                        return false;
                    } else {
                        return true;
                    }
                case 'H':
                    // Sink Hole
                    break;
            }
        }

        return false;
    }

    public static char opponent(char player) {
        if (player == 'G') {
            return 'B';
        } else {
            return 'G';
        }
    }

    public static boolean threeColumn(int col, char player) {
        int count = 0;
        for (int row = 0; row < 4; row++) {
            if (board[row][col].peek().toString().charAt(0) == player) {
                count++;
            }
        }
        if (count >= 3) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean threeRow(int row, char player) {
        int count = 0;
        for (int col = 0; col < 4; col++) {
            if (board[row][col].peek().toString().charAt(0) == player) {
                count++;
            }
        }
        if (count >= 3) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean threeDiagonal(int row, int col, char player) {
        int count = 0;
        int temp_row = row;
        int temp_col = col;
        if (board[row][col].peek().toString().charAt(0) == player) {
            count ++;
        }
        boolean OB = false;
        for (int i = 0; i < 3; i++) {
            if (!OB) {
                temp_row--;
                temp_col--;
                if (temp_row < 0 || temp_col < 0) {
                    temp_row = row;
                    temp_col = col;
                    i--;
                    OB = true;
                    continue;
                } else {
                    if (board[temp_row][temp_col].peek().toString().charAt(0) == player) {
                        count++;
                    }
                }
            } else {
                temp_row++;
                temp_col++;
                if (temp_row > 3 || temp_col > 3) {
                    temp_row = row;
                    temp_col = col;
                    OB = false;
                    break;
                } else {
                    if (board[temp_row][temp_col].peek().toString().charAt(0) == player) {
                        count++;
                    }
                }
            }
        }
        if (count >= 3) {
            return true;
        }

        count = 0;
        temp_row = row;
        temp_col = col;
        if (board[row][col].peek().toString().charAt(0) == player) {
            count ++;
        }
        for (int i = 0; i < 3; i++) {
            if (!OB) {
                temp_row--;
                temp_col++;
                if (temp_row < 0 || temp_col > 3) {
                    temp_row = row;
                    temp_col = col;
                    i--;
                    OB = true;
                    continue;
                } else {
                    if (board[temp_row][temp_col].peek().toString().charAt(0) == player) {
                        count++;
                    }
                }
            } else {
                temp_row++;
                temp_col--;
                if (temp_row > 3 || temp_col < 0) {
                    temp_row = row;
                    temp_col = col;
                    OB = false;
                    break;
                } else {
                    if (board[temp_row][temp_col].peek().toString().charAt(0) == player) {
                        count++;
                    }
                }
            }
        }

        if (count >= 3) {
            return true;
        }

        return false;
    }

    public static void printBoard() {
        System.out.println("   c1 c2 c3 c4");
        for (int i = 0; i < 4; i++) {
            System.out.printf("r%d ", i + 1);
            for (int j = 0; j < 4; j++) {
                System.out.printf("%s|", board[i][j].peek());
            }
            System.out.printf("\n");
        }
        System.out.printf("\n");
    }

}

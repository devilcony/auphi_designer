package org.pentaho.di.cachefile.test;


public class Test
{
    public static void main(String[] args)
    {
        
    }
    public static void knuth(int n, int m)
    {
        int rand  ;
        for (int i = 0 ; i < n ; i++)
        {
            rand = (int)(n * Math.random()) ;
            if ( rand % (n-i) < m)
            {
                System.out.println(i) ;
                m -- ;
            }
        }
    }
    public enum Direction{
        Left(0)
        {
            public int nextColumn(int col)
            {
                return col - 1 ;
            }
        },
        Down(1){
            public int nextRow(int row)
            {
                return row + 1;
            }
        },
        Right(2){
            public int nextColumn(int col)
            {
                return col + 1; 
            }
        },
        Up(3){
            public int nextRow(int row)
            {
                return row - 1 ;
            }
        };
        
        private int directionId ;
        private Direction(int id)
        {
            directionId = id ;
        }
        
        public int nextRow(int row)
        {
            return row ;
        }
        public int nextColumn(int col)
        {
            return col ;
        }        
        
        public Direction nextDirection()
        {
            return Direction.values()[(this.directionId+1)%Direction.values().length] ; 
        }
        
        public boolean canMoveOn(int [][] num , int row, int col )
        {
            int maxRow = num.length ;
            int maxCol = num[0].length ;
            
            int nextRow = nextRow(row) ;
            int nextCol = nextColumn(col) ;
           
            if (nextRow < 0 || nextRow >= maxRow || nextCol < 0 || nextCol >= maxCol)
                return false ;
            
            if (num[nextRow][nextCol] != 0)
                return false;
            
            return true ;            
        }
        
    }
    public static void print()
    {
        int row = 20 ;
        int column = 20 ;
        int [][] num = new int[row][column] ;

        
        Direction currentDir = Direction.Down ;
        int curRow = -1 ;
        int curCol = 0 ;
        
        for (int i = 0 ; i < row*column ; i++)
        {
            if (!currentDir.canMoveOn(num, curRow, curCol))
            {
                currentDir = currentDir.nextDirection() ;
            }
            curRow = currentDir.nextRow(curRow) ;
            curCol = currentDir.nextColumn(curCol) ;
            num[curRow][curCol] = i + 1 ;
        }
        String ss = null ;
        
        StringBuffer sb = new StringBuffer(1024) ;
        for (int i = 0 ; i < row ; i ++)
        {
            
            for (int j = 0 ; j < column ; j ++)
            {
                String number = String.valueOf(num[i][j]) ;
                if (number.length() == 2)
                    number = " " + number ;
                if (number.length() == 1)
                    number = "  " + number;
//                System.out.print(num[i][j] + " ") ;
                sb.append(number).append(" ");
            }
            sb.append("\n") ;
        }
        System.out.println(sb);
        
    }
}

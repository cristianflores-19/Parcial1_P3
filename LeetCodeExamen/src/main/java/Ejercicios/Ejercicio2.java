package Ejercicios;

public class Ejercicio2 {

    public static void main(String[] args) {
        
        int[] ejemplo = {7, 2, 9, 4, 1, 8};
        int[] resultado = buscarSegundoMenorYMayor(ejemplo);
        
        System.out.println("Entrada: [7, 2, 9, 4, 1, 8]");
        System.out.println("Salida: [" + resultado[0] + ", " + resultado[1] + "]");
    }

    public static int[] buscarSegundoMenorYMayor(int[] nums) {
        
        int menor = nums[0];
        int segundoMenor = nums[1];
        
        int mayor = nums[0];
        int segundoMayor = nums[1];
        
        if (menor > segundoMenor) {
            menor = nums[1];
            segundoMenor = nums[0];
        }
        
        if (mayor < segundoMayor) {
            mayor = nums[1];
            segundoMayor = nums[0];
        }
        
        for (int i = 2; i < nums.length; i++) {
            
            if (nums[i] < menor) {
                segundoMenor = menor;
                menor = nums[i];
            } else if (nums[i] < segundoMenor && nums[i] != menor) {
                segundoMenor = nums[i];
            }
            
            if (nums[i] > mayor) {
                segundoMayor = mayor;
                mayor = nums[i];
            } else if (nums[i] > segundoMayor && nums[i] != mayor) {
                segundoMayor = nums[i];
            }
        }
        
        int[] res = new int[2];
        res[0] = segundoMenor;
        res[1] = segundoMayor;
        
        return res;
    }
}
import java.util.Scanner;

public class TiendaBebidas {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        // Asignación de precios a las bebidas
        double precioAlcohol = 5.0;
        double precioJugo = 2.0;
        double precioBatido = 3.0;
        double precioGaseosa = 1.5;
        
        System.out.println("--- Bienvenido a la Tienda de Bebidas ---");
        
        // Verificar edad
        System.out.print("Por favor, ingrese su edad: ");
        int edad = scanner.nextInt();
        
        // Verificar si es cliente frecuente (3 visitas)
        System.out.print("¿Cuántas veces ha visitado la tienda anteriormente?: ");
        int visitas = scanner.nextInt();
        
        // Mostrar menú
        System.out.println("\n--- MENÚ ---");
        System.out.println("1. Alcohol - $" + precioAlcohol);
        System.out.println("2. Jugos - $" + precioJugo);
        System.out.println("3. Batidos - $" + precioBatido);
        System.out.println("4. Gaseosas - $" + precioGaseosa);
        System.out.print("Seleccione la bebida que desea comprar (1-4): ");
        int opcion = scanner.nextInt();
        
        // Validar opción de alcohol para menores de edad
        if (opcion == 1 && edad < 18) {
            System.out.println("\nError: Lo sentimos, debe ser mayor de 18 años para comprar alcohol.");
        } else if (opcion >= 1 && opcion <= 4) {
            // Pedir cantidad
            System.out.print("Ingrese la cantidad que desea comprar: ");
            int cantidad = scanner.nextInt();
            
            // Realizar el cálculo
            double total = 0;
            switch(opcion) {
                case 1:
                    total = cantidad * precioAlcohol;
                    break;
                case 2:
                    total = cantidad * precioJugo;
                    break;
                case 3:
                    total = cantidad * precioBatido;
                    break;
                case 4:
                    total = cantidad * precioGaseosa;
                    break;
                default:
                    // Como ya validamos del 1 al 4, esto es solo por buena práctica
                    System.out.println("Opción no válida.");
                    break;
            }
            
            System.out.println("\nSubtotal a pagar: $" + total);
            
            // Condición de cliente frecuente (descuento del 50%)
            if (visitas >= 3) {
                System.out.println("¡Es cliente frecuente! Se le aplica un descuento del 50%.");
                total = total / 2; // Aplicar 50% de descuento
            } else {
                System.out.println("No aplica descuento de cliente frecuente.");
            }
            
            // Mostrar total final
            System.out.println("Total a pagar: $" + total);
            System.out.println("¡Gracias por su compra!");
            
        } else {
            System.out.println("\nOpción no válida. Por favor seleccione un número del 1 al 4.");
        }
    }
}

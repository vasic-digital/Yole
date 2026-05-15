! SPDX-FileCopyrightText: 2026 Milos Vasic
! SPDX-License-Identifier: Apache-2.0
! iter-58 F2 Phase 6 fixture: Fortran.

program example
  implicit none
  character(len=*), parameter :: name = "Yole"

  call greet(name)

contains

  subroutine greet(who)
    character(len=*), intent(in) :: who
    print *, "Hello, ", trim(who), "!"
  end subroutine greet

end program example
